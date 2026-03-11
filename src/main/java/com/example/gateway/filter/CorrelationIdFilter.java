package com.example.gateway.filter;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest requestWithCorrelationId = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(CORRELATION_ID_HEADER, correlationId))
                .build();

        ServerWebExchange mutatedExchange = exchange
                .mutate()
                .request(requestWithCorrelationId)
                .build();

        String finalCorrelationId = correlationId;
        return chain.filter(mutatedExchange)
                .contextWrite(context -> context.put(CORRELATION_ID_HEADER, finalCorrelationId))
                .doFirst(() -> MDC.put(CORRELATION_ID_HEADER, finalCorrelationId))
                .doFinally(signal -> MDC.remove(CORRELATION_ID_HEADER));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
