package com.bcommerce.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayBeans {

    /**
     * 默认：按客户端 IP 分桶（gw:ip）。
     * profile=stress 且请求头 X-Stress-Shard 非空时：按分片分桶（gw:stress:值），便于单 IP 下 k6 多 VU 拆桶；
     * 仅在 stress 下生效，避免生产环境被伪造头绕过限流。
     */
    @Bean
    KeyResolver ipKeyResolver(Environment env) {
        return exchange -> {
            if (env.matchesProfiles("stress")) {
                String shard = exchange.getRequest().getHeaders().getFirst("X-Stress-Shard");
                if (shard != null && !shard.isBlank()) {
                    return Mono.just("gw:stress:" + shard.trim());
                }
            }
            var addr = exchange.getRequest().getRemoteAddress();
            String ip = "unknown";
            if (addr != null && addr.getAddress() != null) {
                ip = addr.getAddress().getHostAddress();
            }
            return Mono.just("gw:" + ip);
        };
    }
}
