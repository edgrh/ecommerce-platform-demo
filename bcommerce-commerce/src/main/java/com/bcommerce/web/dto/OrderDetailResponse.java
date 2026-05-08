package com.bcommerce.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        long id,
        String orderNo,
        int totalCent,
        String status,
        String orderType,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {}
