package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSpu;
import java.time.LocalDateTime;

public record ProductSpuDetailResponse(
        long id, String title, String subtitle, String detail, String status, LocalDateTime createdAt) {

    public static ProductSpuDetailResponse from(ProductSpu p) {
        return new ProductSpuDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getSubtitle() != null ? p.getSubtitle() : "",
                p.getDetail() != null ? p.getDetail() : "",
                p.getStatus(),
                p.getCreatedAt());
    }
}
