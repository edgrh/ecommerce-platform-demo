package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProductSpu {
    private Long id;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String detail;
    private Long merchantId;
    private String status;
    private LocalDateTime createdAt;
    /** 在售 SKU 最低价（分）；由查询子查询填充，非表字段。 */
    private Integer minPriceCent;
}
