package com.bcommerce.trade;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentMockClient {

    @CircuitBreaker(name = "paymentMock", fallbackMethod = "mockPayFallback")
    public void mockConfirm(Long orderId) {
        int r = ThreadLocalRandom.current().nextInt(100);
        if (r < 3) {
            throw new IllegalStateException("channel_timeout");
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5, 25));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted");
        }
        log.debug("mock payment ok orderId={}", orderId);
    }

    @SuppressWarnings("unused")
    void mockPayFallback(Long orderId, Throwable cause) {
        throw new IllegalStateException("payment_circuit_open");
    }
}
