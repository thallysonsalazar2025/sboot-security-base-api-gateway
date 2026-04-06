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
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String headerCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        final String correlationId = (headerCorrelationId == null || headerCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : headerCorrelationId;

        ServerHttpRequest requestWithCorrelationId = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate().request(requestWithCorrelationId).build();
        mutatedExchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(mutatedExchange)
                .contextWrite(context -> context.put(CORRELATION_ID_HEADER, correlationId))
                .doFirst(() -> {
                    MDC.put(CORRELATION_ID_HEADER, correlationId);
                    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
                })
                .doFinally(signal -> {
                    MDC.remove(CORRELATION_ID_HEADER);
                    MDC.remove(CORRELATION_ID_MDC_KEY);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
