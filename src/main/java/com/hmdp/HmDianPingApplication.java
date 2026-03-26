package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;

@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        SpringApplication.run(HmDianPingApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(@Autowired RedisProperties redisProperties){
        return args ->{
            System.out.println("================== Redis Connection Details ==================");
            System.out.println("### Spring Boot is ACTUALLY connecting to Redis at: " +
                    redisProperties.getHost() + ":" + redisProperties.getPort() + " ###");
            System.out.println("============================================================");
        };
    }
}
