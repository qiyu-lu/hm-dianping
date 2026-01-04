package com.hmdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLuaScript {
    public static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(
                new ClassPathResource("lua/unlock.lua")
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
}
