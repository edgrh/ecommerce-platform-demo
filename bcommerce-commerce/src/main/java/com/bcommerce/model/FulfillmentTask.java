package com.bcommerce.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FulfillmentTask {
    private Long id;
    private Long orderId;
    private String status;
    private String logisticsNo;
    private LocalDateTime updatedAt;
}
