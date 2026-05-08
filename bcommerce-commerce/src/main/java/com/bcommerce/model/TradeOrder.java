package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TradeOrder {
    private Long id;
    private String orderNo;
    private Long userId;
    private String orderType;
    private Integer totalCent;
    private String status;
    private Long seckillActivityId;
    private LocalDateTime createdAt;
}
