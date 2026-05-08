package com.bcommerce.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayBeans {

    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> {
            var addr = exchange.getRequest().getRemoteAddress();
            String ip = "unknown";
            if (addr != null && addr.getAddress() != null) {
                ip = addr.getAddress().getHostAddress();
            }
            return Mono.just("gw:" + ip);
        };
    }
}
