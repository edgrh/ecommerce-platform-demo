package com.bcommerce.web;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
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

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Map<String, Object>> handleRedisConn(RedisConnectionFailureException ex) {
        log.warn("Redis 连接失败: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        Map.of(
                                "error",
                                "无法连接 Redis，请确认 Docker 已启动且端口与 application.yml 一致（默认 localhost:6380）"));
    }

    /**
     * Lettuce 等抛出的多数 Redis 运行时异常包装为此类，同样继承 {@link DataAccessException}，
     * 若只注册泛化的 DataAccess 处理器会被误报成「MySQL」。
     */
    @ExceptionHandler(RedisSystemException.class)
    public ResponseEntity<Map<String, Object>> handleRedisSystem(RedisSystemException ex) {
        log.warn("Redis 执行异常: {}", ex.getMessage());
        log.debug("Redis 执行异常详情", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        Map.of(
                                "error",
                                "Redis 不可用或执行失败，请确认 Docker 已启动且映射端口与 application.yml 一致（默认 localhost:6380）"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null) {
            String cn = cause.getClass().getName();
            // 部分 Lettuce/客户端异常会包装进 Spring DAO 异常树，易与 MySQL 混淆
            if (cn.startsWith("io.lettuce")
                    || cn.startsWith("redis.clients")
                    || cn.contains("redis.clients.jedis")) {
                log.warn("数据访问异常（根因为 Redis 客户端）: {}", cause.getMessage());
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(
                                Map.of(
                                        "error",
                                        "Redis 不可用或执行失败，请确认 Docker 已启动且映射端口与 application.yml 一致（默认 localhost:6380）"));
            }
        }
        log.error("数据访问异常（多为 JDBC/MySQL）", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        Map.of(
                                "error",
                                "数据库访问失败，请确认 MySQL 已启动且端口与配置一致（默认 localhost:3308）；若控制台堆栈含 redis/lettuce，请检查 Redis（默认 6380）"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "服务异常，请查看控制台日志"));
    }
}
