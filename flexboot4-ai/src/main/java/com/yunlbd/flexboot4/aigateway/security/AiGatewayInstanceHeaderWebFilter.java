package com.yunlbd.flexboot4.aigateway.security;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(-100)
public class AiGatewayInstanceHeaderWebFilter implements WebFilter {

    private static final String INSTANCE_ID = UUID.randomUUID().toString();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.startsWith("/api/ai/")) {
            exchange.getResponse().getHeaders().set("X-AI-Gateway-Instance", INSTANCE_ID);
        }
        return chain.filter(exchange);
    }
}

