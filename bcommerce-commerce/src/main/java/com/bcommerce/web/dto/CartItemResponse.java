package com.bcommerce.web.dto;

public record CartItemResponse(long skuId, long spuId, String title, int unitPriceCent, int quantity) {}

