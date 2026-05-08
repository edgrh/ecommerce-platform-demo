package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SettlementEntry {
    private Long id;
    private Long orderId;
    private Long merchantId;
    private Integer amountCent;
    private Integer feeCent;
    private String status;
    private LocalDateTime createdAt;
}
