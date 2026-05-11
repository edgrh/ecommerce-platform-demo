package com.bcommerce.web.dto;

import com.bcommerce.model.ProductSku;
import com.bcommerce.support.SkuSpecSummary;

public record ProductSkuBriefResponse(
        long id,
        String skuCode,
        int priceCent,
        int stock,
        String specSummary,
        /** Raw JSON for storefront spec pickers (storage / color, etc.). */
        String specJson) {
    public static ProductSkuBriefResponse from(ProductSku s) {
        return new ProductSkuBriefResponse(
                s.getId(),
                s.getSkuCode() != null ? s.getSkuCode() : "",
                s.getPriceCent() != null ? s.getPriceCent() : 0,
                s.getStock() != null ? s.getStock() : 0,
                SkuSpecSummary.of(s.getSpecJson()),
                s.getSpecJson() != null ? s.getSpecJson() : "");
    }
}

