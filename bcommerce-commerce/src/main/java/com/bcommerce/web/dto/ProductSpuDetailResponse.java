package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSpu;
import java.time.LocalDateTime;
import java.util.List;

public record ProductSpuDetailResponse(
        long id,
        String title,
        String subtitle,
        String detail,
        String status,
        LocalDateTime createdAt,
        List<ProductSkuBriefResponse> skus) {

    public static ProductSpuDetailResponse from(ProductSpu p, List<ProductSkuBriefResponse> skus) {
        return new ProductSpuDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getSubtitle() != null ? p.getSubtitle() : "",
                p.getDetail() != null ? p.getDetail() : "",
                p.getStatus(),
                p.getCreatedAt(),
                skus != null ? skus : List.of());
    }
}
