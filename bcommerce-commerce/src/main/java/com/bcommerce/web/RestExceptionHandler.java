package com.bcommerce.web;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValid(MethodArgumentNotValidException ex) {
        var f = ex.getBindingResult().getFieldError();
        String msg = f != null && f.getDefaultMessage() != null ? f.getDefaultMessage() : "Invalid parameters";
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitOpen(CallNotPermittedException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "支付通道熔断，请稍后再试"));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleAmqp(AmqpException ex) {
        log.warn("RabbitMQ 异常: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "消息队列不可用，请确认 Docker 中 RabbitMQ 已启动"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "服务异常，请查看控制台日志"));
    }
}
