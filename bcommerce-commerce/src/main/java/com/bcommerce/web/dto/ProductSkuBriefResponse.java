package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSku;

public record ProductSkuBriefResponse(long id, String skuCode, int priceCent, int stock) {
    public static ProductSkuBriefResponse from(ProductSku s) {
        return new ProductSkuBriefResponse(
                s.getId(),
                s.getSkuCode() != null ? s.getSkuCode() : "",
                s.getPriceCent() != null ? s.getPriceCent() : 0,
                s.getStock() != null ? s.getStock() : 0);
    }
}

