package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.User;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.RedisLuaScript;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker  redisIdWorker;

    @Lazy // 为了避免循环依赖，这里使用 @Lazy 注解
    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    private final BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // 在类初始化完毕后，就立即执行这个任务
    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    //获取队列中的订单信息
                    VoucherOrder voucherOrder = orderTasks.take();
                    //创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    log.error("阻塞队列中处理订单异常" , e);
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        //获取用户id
        Long userId = voucherOrder.getUserId();
        //创建锁对象
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock();
        if (!isLock) {
            //获取锁失败，直接返回错误信息
            log.error("不允许重复下单！");
            return;
        }
        try{
            //注意：由于是this.createVoucherOrder调用，事务想要生效需要通过代理对象调用
            // 我们在类中注入了IVoucherOrderService的代理对象来解决此问题
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Result seckillVoucher(Long voucherId) {

        Long userId = UserHolder.getUser().getId();
        //1. 执行lua脚本
        Long result = stringRedisTemplate.execute(
                RedisLuaScript.SECKILL_SCRIPT,
                Arrays.asList(SECKILL_STOCK_KEY + voucherId,
                        SECKILL_ORDER_KEY + voucherId),
                userId.toString()
        );
        log.info("此时的vouchers 的id 是：" + voucherId);
        //2. 判断结果是否为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1. 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "您已抢过该优惠券");
        }
        //2.2 为0，有购买资格，把下单信息保存到阻塞队列中
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        // 放入阻塞队列
        orderTasks.add(voucherOrder);
        //3. 返回订单id
        return Result.ok(orderId);
    }
//    @Override
//    public Result seckillVoucher(Long voucherId){
//        //查询优惠券信息
//        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
//        //判断秒杀时间
//        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now()))
//            return Result.fail("秒杀未开始");
//        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now()))
//            return Result.fail("秒杀已结束");
//        //当前时间在秒杀时间范围内，则判断库存是否充足
//        if(seckillVoucher.getStock() < 1)
//            return Result.fail("秒杀券库存不足");
//        //如果库存充足，则扣减库存
//        //一人一单校验
//        Long userId = UserHolder.getUser().getId();
//        // 3. redis锁实现一人一单
//        String lockKey = "order:" + userId;
//        org.redisson.api.RLock lock = redissonClient.getLock("lock:" + lockKey);
//        boolean success = lock.tryLock();
//
//        if(!success){
//            return Result.fail("不能重复多次下单");//这里抢购就不采用重复进行的策略，而是采用直接终止
//        }
//        try{
//            //  必须通过代理对象调用
//            log.info("进入 synchronized，port={}, userId={}",
//                    serverPort, userId);
//            return voucherOrderService.createVoucherOrder(voucherId);
//        }
//        finally{
//            lock.unlock();
//        }
//
//    }
    /**
     * 真正创建订单的方法（负责事务）
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        //因为是异步线程不一样，不用使用原来的方式getid
        Long userId = voucherOrder.getUserId();

        // 4. 一人一单校验
        int count = this.query().eq("user_id", userId)
                .eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("用户 {} 重复下单！", userId);
            return;
        }

        // 5. 扣减库存（乐观锁）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0) // where id =
                .update();// ? and stock > 0

        if (!success) {
            log.error("库存不足");
            return;
        }

        // 6. 创建订单
        // voucherOrder 对象已经由主线程创建并放入了队列，这里直接保存即可
        this.save(voucherOrder);
    }
}
