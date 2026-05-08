package com.bcommerce.web.dto;

import com.bcommerce.model.SeckillActivity;
import java.time.LocalDateTime;

public record SeckillActivityResponse(
        long id,
        long skuId,
        String name,
        int seckillPriceCent,
        int remaining,
        int limitPerUser,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status) {

    public static SeckillActivityResponse from(SeckillActivity a) {
        int left = a.getTotalStock() - a.getSoldStock();
        return new SeckillActivityResponse(
                a.getId(),
                a.getSkuId(),
                a.getName(),
                a.getSeckillPriceCent(),
                Math.max(left, 0),
                a.getLimitPerUser(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus());
    }
}
