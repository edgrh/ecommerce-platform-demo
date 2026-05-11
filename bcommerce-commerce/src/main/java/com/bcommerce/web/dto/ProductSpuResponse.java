package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSpu;
import java.time.LocalDateTime;

public record ProductSpuResponse(
        long id,
        String title,
        String subtitle,
        String status,
        LocalDateTime createdAt,
        /** Lowest SKU price in cents; null if no SKU row. */
        Long minPriceCent) {

    public static ProductSpuResponse from(ProductSpu p) {
        Long mpc = p.getMinPriceCent() == null ? null : p.getMinPriceCent().longValue();
        return new ProductSpuResponse(
                p.getId(), p.getTitle(), p.getSubtitle(), p.getStatus(), p.getCreatedAt(), mpc);
    }

    public static ProductSpuResponse from(com.bcommerce.product.es.ProductSpuDocument d) {
        return new ProductSpuResponse(
                d.getId(), d.getTitle(), d.getSubtitle(), d.getStatus(), d.getCreatedAt(), null);
    }

    public ProductSpuResponse withMinPriceCent(Long cents) {
        return new ProductSpuResponse(id, title, subtitle, status, createdAt, cents);
    }
}
