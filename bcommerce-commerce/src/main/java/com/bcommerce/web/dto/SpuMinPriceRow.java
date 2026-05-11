package com.bcommerce.web.dto;

/** Batch lookup: lowest SKU price per SPU (for list/search enrichment). */
public record SpuMinPriceRow(Long spuId, Integer priceCent) {}
