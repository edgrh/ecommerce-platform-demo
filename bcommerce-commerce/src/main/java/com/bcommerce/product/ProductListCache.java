package com.bcommerce.product;

import com.bcommerce.config.BcommerceProperties;
import com.bcommerce.web.dto.ProductSpuResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductListCache {

    private static final String KEY = "catalog:product:shelf:v1";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final BcommerceProperties properties;

    public List<ProductSpuResponse> getShelf() {
        try {
            String json = redis.opsForValue().get(KEY);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public void putShelf(List<ProductSpuResponse> list) throws Exception {
        int sec = Math.max(5, properties.getCache().getProductListTtlSeconds());
        String json = objectMapper.writeValueAsString(list);
        redis.opsForValue().set(KEY, json, Duration.ofSeconds(sec));
    }

    public void evictShelf() {
        redis.delete(KEY);
    }
}
