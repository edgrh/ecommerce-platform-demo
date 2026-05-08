package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SeckillActivity {
    private Long id;
    private Long skuId;
    private String name;
    private Integer seckillPriceCent;
    private Integer totalStock;
    private Integer soldStock;
    private Integer limitPerUser;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
