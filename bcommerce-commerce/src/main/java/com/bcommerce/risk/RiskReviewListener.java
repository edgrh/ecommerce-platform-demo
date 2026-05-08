package com.bcommerce.risk;

import com.bcommerce.config.RabbitConfig;
import com.bcommerce.mapper.RiskAuditTaskMapper;
import com.bcommerce.model.RiskAuditTask;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskReviewListener {

    private final RiskAuditTaskMapper riskAuditTaskMapper;

    @RabbitListener(queues = RabbitConfig.RISK_QUEUE)
    public void onMessage(String body) {
        String[] parts = body.split(",");
        long orderId = Long.parseLong(parts[0].trim());
        long userId = Long.parseLong(parts[1].trim());

        RiskAuditTask task = new RiskAuditTask();
        task.setOrderId(orderId);
        task.setUserId(userId);
        task.setRiskScore(ThreadLocalRandom.current().nextInt(5, 35));
        task.setStatus("PASS");
        task.setRemark("async_scan_ok");
        riskAuditTaskMapper.insert(task);
        log.info("risk audit orderId={} userId={} taskId={}", orderId, userId, task.getId());
    }
}
