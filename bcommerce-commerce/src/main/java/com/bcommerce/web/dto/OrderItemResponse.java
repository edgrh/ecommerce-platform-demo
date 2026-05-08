package com.bcommerce.web.dto;

public record OrderItemResponse(long skuId, int quantity, int unitPriceCent, String titleSnapshot) {}
