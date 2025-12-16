package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.ShopBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopBloomFilter shopBloomFilter;

    @Override
    public Result queryShopById(Long id){
        //构造查询的id，在redis中的key
        String shopKey = CACHE_SHOP_KEY + id;

        //  布隆过滤器拦截
        if (!shopBloomFilter.mightContain(id)) {
           log.debug("布隆过滤器");
            return Result.fail("布隆过滤器：店铺不存在");
        }
        //先设置在redis中存的是json格式
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        if(StrUtil.isNotBlank(shopJson)){
            log.debug("从redis中进行查询商铺");
            if(shopJson.equals("null")){
                return Result.fail("查询商铺不存在");
            }
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        //现在经过上面的操作确定：
        // 布隆过滤器说id可能存在，不是一定存在，（布隆过滤器说不存在就一定是不存在，说存在而不一定一定存在）
        // redis 中当前没有缓存结果，既可能是从来没有写过，也可能是写过过期或被清理了
        /// 那么此时就要对 缓存击穿进行处理
        String lockKey = LOCK_SHOP_KEY + id; // // 给每家店单独一把锁，互不影响
        boolean locked = false;
        try{
            //尝试拿锁
            //setIfAbsent = Redis 命令 SET NX（只有不存在才成功）
            //成功返回 true → 当前线程抢到锁；失败返回 false → 被别人占着。
            locked = Boolean.TRUE.equals(
                    stringRedisTemplate.opsForValue()
                            .setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS)
            );
            if(!locked){
                //没抢到 等待 50 ms 后重试一次（简单自旋）
                try{Thread.sleep(50);} catch (InterruptedException ignored) {}
                return queryShopById(id); // 重新读一次缓存（大概率已有了）
                //这里用“简单自旋”：等一会儿再递归调自己，实际就是让线程排队重试
            }
            //抢到锁，重新读缓存 可能在你排队那 50ms 里，前一个人已把数据写进 Redis，直接拿就行，不用再查 DB。
            shopJson = stringRedisTemplate.opsForValue().get(shopKey);
            if(StrUtil.isNotBlank(shopJson)){
                if("null".equals(shopJson)) return Result.fail("抢到锁后，重新读缓存，店铺不存在");
                return Result.ok(JSONUtil.toBean(shopJson, Shop.class));
            }
            //确实没有，查数据库 到这一步只有一个线程在执行，所以 DB 不会被打爆。
            Shop shop = getById(id);
            //模拟重建延时
            Thread.sleep(300);
            if(shop == null){
                stringRedisTemplate.opsForValue().set(shopKey, "null", CACHE_NULL_TTL,  TimeUnit.MINUTES);
                return Result.fail("确实没有，查数据库发现查询商铺不存在");
            }
            // 把结果写回 Redis，给后面排队的人用
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
            return Result.ok(shop);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }
        finally {
            //释放锁
            if(locked){
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    @Override
    @Transactional
    public Result updateShop(Shop shop){
        //根据id修改店铺时，先修改数据库，再删除缓存
        Long shopId = shop.getId();
        if(shopId == null){
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
