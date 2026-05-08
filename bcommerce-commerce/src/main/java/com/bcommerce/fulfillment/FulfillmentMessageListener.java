package com.bcommerce.fulfillment;

import com.bcommerce.config.RabbitConfig;
import com.bcommerce.mapper.FulfillmentTaskMapper;
import com.bcommerce.model.FulfillmentTask;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FulfillmentMessageListener {

    private final FulfillmentTaskMapper fulfillmentTaskMapper;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void onOrderCreated(String body) {
        long orderId = Long.parseLong(body.trim());
        FulfillmentTask task = new FulfillmentTask();
        task.setOrderId(orderId);
        task.setStatus("WMS_ACCEPTED");
        task.setLogisticsNo("SF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        fulfillmentTaskMapper.updateByOrderId(task);
        log.info("fulfillment accepted orderId={} waybill={}", orderId, task.getLogisticsNo());
    }
}
