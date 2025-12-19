package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private CacheClient  cacheClient;

    @Test
    void testSaveShop2RedisCache() throws InterruptedException {
        shopService.saveShop2RedisCache(1L,10L);
    }

    @Test
    void testCacheClient(){
        Shop shop = new Shop()
                .setId(100L).setName("test").setTypeId(100L).setArea("test");
        cacheClient.queryWithPassThrough("test", 100L, Shop.class,
                shopService::getById,
                30L,
                TimeUnit.SECONDS);
//        log.info("shop:{} expireTime: {}",shop, System.currentTimeMillis() + 30_000L);
    }
}
