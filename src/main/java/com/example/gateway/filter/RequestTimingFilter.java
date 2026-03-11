package com.example.gateway.filter;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestTimingFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time-Ms";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant start = Instant.now();
        String path = exchange.getRequest().getURI().getPath();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long elapsedMillis = Duration.between(start, Instant.now()).toMillis();
                    exchange.getResponse().getHeaders().set(RESPONSE_TIME_HEADER, String.valueOf(elapsedMillis));
                    LOGGER.info("Request timing path={} method={} elapsedMs={}",
                            path,
                            exchange.getRequest().getMethod(),
                            elapsedMillis);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}
