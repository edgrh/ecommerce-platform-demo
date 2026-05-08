package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSpu;
import com.bcommerce.product.es.ProductSpuDocument;
import java.time.LocalDateTime;

public record ProductSpuResponse(
        long id, String title, String subtitle, String status, LocalDateTime createdAt) {

    public static ProductSpuResponse from(ProductSpu p) {
        return new ProductSpuResponse(p.getId(), p.getTitle(), p.getSubtitle(), p.getStatus(), p.getCreatedAt());
    }

    public static ProductSpuResponse from(ProductSpuDocument d) {
        return new ProductSpuResponse(d.getId(), d.getTitle(), d.getSubtitle(), d.getStatus(), d.getCreatedAt());
    }
}
