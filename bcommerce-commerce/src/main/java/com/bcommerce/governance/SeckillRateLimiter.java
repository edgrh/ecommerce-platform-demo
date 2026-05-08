package com.bcommerce.governance;

import com.bcommerce.config.BcommerceProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeckillRateLimiter {

    private static final String KEY_PREFIX = "rate:seckill:user:";

    private final StringRedisTemplate redis;
    private final BcommerceProperties properties;

    public boolean allow(Long userId) {
        if (userId == null) {
            return true;
        }
        String key = KEY_PREFIX + userId;
        Long n = redis.opsForValue().increment(key);
        if (n != null && n == 1) {
            redis.expire(key, Duration.ofMinutes(1));
        }
        int max = Math.max(1, properties.getSeckill().getRatePerMinute());
        return n != null && n <= max;
    }
}
