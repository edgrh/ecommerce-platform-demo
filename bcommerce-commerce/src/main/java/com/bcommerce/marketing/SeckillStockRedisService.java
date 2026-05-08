package com.bcommerce.marketing;

import com.bcommerce.mapper.SeckillActivityMapper;
import com.bcommerce.model.SeckillActivity;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeckillStockRedisService {

    private static final String KEY_PREFIX = "seckill:stock:";
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>();

    static {
        RESERVE.setScriptText(
                """
                local raw = redis.call('GET', KEYS[1])
                if not raw then return -1 end
                local current = tonumber(raw)
                local n = tonumber(ARGV[1])
                if current < n then return -2 end
                return redis.call('DECRBY', KEYS[1], n)
                """);
        RESERVE.setResultType(Long.class);
        RELEASE.setScriptText(
                """
                local raw = redis.call('GET', KEYS[1])
                if not raw then return -1 end
                return redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
                """);
        RELEASE.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final SeckillActivityMapper activityMapper;

    public void reloadAllActivities() {
        for (SeckillActivity a : activityMapper.listOnline()) {
            refreshActivity(a.getId());
        }
    }

    public void refreshActivity(Long activityId) {
        SeckillActivity a = activityMapper.findById(activityId);
        if (a == null) {
            return;
        }
        int left = a.getTotalStock() - a.getSoldStock();
        redis.opsForValue().set(key(activityId), String.valueOf(Math.max(left, 0)));
    }

    public Long reserve(Long activityId, int qty) {
        return redis.execute(RESERVE, Collections.singletonList(key(activityId)), String.valueOf(qty));
    }

    public Long release(Long activityId, int qty) {
        return redis.execute(RELEASE, Collections.singletonList(key(activityId)), String.valueOf(qty));
    }

    private static String key(Long id) {
        return KEY_PREFIX + id;
    }
}
