package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RiskAuditTask {
    private Long id;
    private Long orderId;
    private Long userId;
    private Integer riskScore;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
}
