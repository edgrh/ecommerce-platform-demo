package com.bcommerce.web.dto;

import com.bcommerce.model.RiskAuditTask;
import java.time.LocalDateTime;

public record RiskAuditTaskResponse(
        long id, long orderId, int riskScore, String status, String remark, LocalDateTime createdAt) {

    public static RiskAuditTaskResponse from(RiskAuditTask t) {
        return new RiskAuditTaskResponse(
                t.getId(),
                t.getOrderId(),
                t.getRiskScore() != null ? t.getRiskScore() : 0,
                t.getStatus(),
                t.getRemark(),
                t.getCreatedAt());
    }
}
