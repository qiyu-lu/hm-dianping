package com.localdeals.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.localdeals.dto.Result;
import com.localdeals.entity.ShopType;
import com.localdeals.mapper.ShopTypeMapper;
import com.localdeals.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.localdeals.utils.RedisConstants.CACHE_SHOP_TYPE_LIST_KEY;
import static com.localdeals.utils.RedisConstants.CACHE_SHOP_TYPE_LIST_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList(){
        String cachJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_LIST_KEY);

        if(StrUtil.isNotBlank(cachJson)){
            List<ShopType> typeList = JSONUtil.toList(cachJson, ShopType.class);
            return typeList;
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();

        if(CollectionUtil.isNotEmpty(typeList)){
            stringRedisTemplate.opsForValue().set(
                    CACHE_SHOP_TYPE_LIST_KEY,
                    JSONUtil.toJsonStr(typeList),
                    CACHE_SHOP_TYPE_LIST_TTL,
                    TimeUnit.MINUTES);
            return typeList;
        }
        return typeList;
    }
}
