package com.bcommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bcommerce")
public class BcommerceProperties {

    private final Jwt jwt = new Jwt();
    private final Seckill seckill = new Seckill();
    private final Cache cache = new Cache();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Seckill {
        private int ratePerMinute = 30;
    }

    @Getter
    @Setter
    public static class Cache {
        private int productListTtlSeconds = 45;
    }
}
