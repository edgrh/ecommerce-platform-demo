package com.bcommerce.web.dto;

import com.bcommerce.model.SeckillActivity;
import java.time.LocalDateTime;

public record SeckillActivityResponse(
        long id,
        long skuId,
        /** 绑定 SKU 所属 SPU，用于跳转商品详情；未解析到则为 0 */
        long spuId,
        String name,
        int seckillPriceCent,
        int remaining,
        int limitPerUser,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        /** 活动绑定的 SPU 标题（演示中每个活动固定一款商品） */
        String productTitle) {

    public static SeckillActivityResponse from(SeckillActivity a, String productTitle, long spuId) {
        int left = a.getTotalStock() - a.getSoldStock();
        return new SeckillActivityResponse(
                a.getId(),
                a.getSkuId(),
                spuId,
                a.getName(),
                a.getSeckillPriceCent(),
                Math.max(left, 0),
                a.getLimitPerUser(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                productTitle != null ? productTitle : "");
    }
}
