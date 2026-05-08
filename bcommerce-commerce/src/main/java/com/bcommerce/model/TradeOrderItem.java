package com.bcommerce.model;

import lombok.Data;

@Data
public class TradeOrderItem {
    private Long id;
    private Long orderId;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Integer unitPriceCent;
    private String titleSnapshot;
}
