package com.example.gateway.filter;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final int maxRequestsPerWindow;
    private final Duration windowDuration;
    private final Map<String, CounterWindow> requestCounters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupEpochMilli = new AtomicLong(0);

    public RateLimitingFilter(
            @Value("${gateway.rate-limit.max-requests-per-window:100}") int maxRequestsPerWindow,
            @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowDuration = Duration.ofSeconds(windowSeconds);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant now = Instant.now();
        evictExpiredCounters(now);

        String clientKey = resolveClientKey(exchange);
        CounterWindow counterWindow = requestCounters.compute(clientKey, (key, existingWindow) -> {
            if (existingWindow == null || now.isAfter(existingWindow.windowStart().plus(windowDuration))) {
                return new CounterWindow(now, new AtomicInteger(1));
            }
            existingWindow.requestCount().incrementAndGet();
            return existingWindow;
        });

        int currentCount = counterWindow.requestCount().get();
        if (currentCount > maxRequestsPerWindow) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("Retry-After", String.valueOf(windowDuration.toSeconds()));
            return response.setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }

        return remoteAddress.getAddress().getHostAddress();
    }

    private void evictExpiredCounters(Instant now) {
        long nowEpochMilli = now.toEpochMilli();
        long lastCleanup = lastCleanupEpochMilli.get();
        long cleanupIntervalMillis = windowDuration.toMillis();

        if (nowEpochMilli - lastCleanup < cleanupIntervalMillis) {
            return;
        }

        if (!lastCleanupEpochMilli.compareAndSet(lastCleanup, nowEpochMilli)) {
            return;
        }

        requestCounters.entrySet().removeIf(entry -> now.isAfter(entry.getValue().windowStart().plus(windowDuration)));
    }

    private record CounterWindow(Instant windowStart, AtomicInteger requestCount) {
    }
}
