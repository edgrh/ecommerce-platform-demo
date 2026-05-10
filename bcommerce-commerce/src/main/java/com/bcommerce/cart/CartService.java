package com.bcommerce.cart;

import com.bcommerce.mapper.ProductSkuMapper;
import com.bcommerce.mapper.ProductSpuMapper;
import com.bcommerce.model.ProductSku;
import com.bcommerce.model.ProductSpu;
import com.bcommerce.web.dto.CartItemResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final StringRedisTemplate redis;
    private final ProductSkuMapper skuMapper;
    private final ProductSpuMapper spuMapper;

    private static String key(long userId) {
        return "cart:user:" + userId;
    }

    @Transactional
    public void add(long userId, long skuId, int qty) {
        if (qty <= 0 || qty > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
        ProductSku sku = skuMapper.findById(skuId);
        if (sku == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU not found");

        redis.opsForHash().increment(key(userId), String.valueOf(skuId), qty);
    }

    @Transactional
    public void setQty(long userId, long skuId, int qty) {
        if (qty < 0 || qty > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
        if (qty == 0) {
            remove(userId, skuId);
            return;
        }
        ProductSku sku = skuMapper.findById(skuId);
        if (sku == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SKU not found");
        redis.opsForHash().put(key(userId), String.valueOf(skuId), String.valueOf(qty));
    }

    @Transactional
    public void remove(long userId, long skuId) {
        redis.opsForHash().delete(key(userId), String.valueOf(skuId));
    }

    @Transactional
    public void clear(long userId) {
        redis.delete(key(userId));
    }

    public List<CartItemResponse> list(long userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(key(userId));
        if (entries == null || entries.isEmpty()) return List.of();

        List<CartItemResponse> out = new ArrayList<>();
        for (var e : entries.entrySet()) {
            long skuId;
            int qty;
            try {
                skuId = Long.parseLong(String.valueOf(e.getKey()));
                qty = Integer.parseInt(String.valueOf(e.getValue()));
            } catch (Exception ignore) {
                continue;
            }
            if (qty <= 0) continue;

            ProductSku sku = skuMapper.findById(skuId);
            if (sku == null) continue;
            ProductSpu spu = spuMapper.findById(sku.getSpuId());
            String title = spu != null ? (spu.getTitle() != null ? spu.getTitle() : "") : "";

            out.add(
                    new CartItemResponse(
                            skuId,
                            sku.getSpuId() != null ? sku.getSpuId() : 0L,
                            title,
                            sku.getPriceCent() != null ? sku.getPriceCent() : 0,
                            qty));
        }
        out.sort(Comparator.comparingLong(CartItemResponse::spuId).thenComparingLong(CartItemResponse::skuId));
        return out;
    }
}

