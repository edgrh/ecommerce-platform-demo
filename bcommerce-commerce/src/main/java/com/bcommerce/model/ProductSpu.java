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
    /** Lowest SKU price (yen×100); filled by list/search SQL, not a table column on spu. */
    private Integer minPriceCent;
}
