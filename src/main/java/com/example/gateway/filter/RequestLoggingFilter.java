package com.example.gateway.filter;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String queryParams = request.getQueryParams().toSingleValueMap().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        LOGGER.info("Incoming request method={} path={} query={} correlationId={} remoteAddress={}",
                request.getMethod(),
                request.getURI().getPath(),
                queryParams,
                correlationId,
                request.getRemoteAddress());

        return chain.filter(exchange)
                .doOnSuccess(unused -> LOGGER.info("Request completed status={} path={} correlationId={}",
                        exchange.getResponse().getStatusCode(),
                        request.getURI().getPath(),
                        correlationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
