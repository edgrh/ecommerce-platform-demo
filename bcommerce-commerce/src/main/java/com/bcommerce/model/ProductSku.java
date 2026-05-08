package com.bcommerce.model;

import lombok.Data;

@Data
public class ProductSku {
    private Long id;
    private Long spuId;
    private String skuCode;
    private String specJson;
    private Integer priceCent;
    private Integer stock;
    private Integer sold;
}
