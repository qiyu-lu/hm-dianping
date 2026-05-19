package com.localdeals.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "local-deals.seckill")
public class SeckillProperties {

    private Stream stream = new Stream();

    @Data
    public static class Stream {
        private String key = "stream.orders";
        private String group = "g1";
        private String consumer = "c1";
        private String deadLetterKey = "stream.orders.dlq";
        private String retryKeyPrefix = "seckill:stream:retry:";
        private int readCount = 10;
        private int workerCount = 1;
        private int maxRetry = 3;
        private Duration block = Duration.ofSeconds(2);
    }
}
