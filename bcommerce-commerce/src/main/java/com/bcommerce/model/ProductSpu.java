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
}
