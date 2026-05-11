package com.bcommerce.gateway;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/503")
    public Mono<ResponseEntity<Map<String, String>>> commerceDown() {
        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(
                                Map.of(
                                        "error",
                                        "commerce_unavailable_or_open_circuit",
                                        "hint",
                                        "网关熔断或 commerce 不可达：确认 8081（commerce）、6380（Redis）可用后重启 gateway。")));
    }
}
