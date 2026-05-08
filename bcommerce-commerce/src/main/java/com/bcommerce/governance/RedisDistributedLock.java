package com.bcommerce.governance;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private static final String PREFIX = "lock:";

    private final StringRedisTemplate redis;

    public boolean tryLock(String name, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(PREFIX + name, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String name) {
        redis.delete(PREFIX + name);
    }
}
