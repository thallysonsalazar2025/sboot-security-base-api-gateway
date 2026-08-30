package com.example.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

@Component
public class TrustedTenantHeaderFilter implements WebFilter, Ordered {

    static final String TENANT_HEADER = "X-Authenticated-Tenant-Id";
    static final String EMPLOYEE_HEADER = "X-Authenticated-Employee-Id";
    private static final PathPattern POINT_SYNC_PATH =
            PathPatternParser.defaultInstance.parse("/api/time-clock/events/sync/**");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        RequestPath requestPath = exchange.getRequest().getPath();
        if (!POINT_SYNC_PATH.matches(requestPath.pathWithinApplication())) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    String companyId = authentication.getToken().getClaimAsString("companyId");
                    if (companyId == null || companyId.isBlank()) {
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }

                    String employeeId = authentication.getToken().getClaimAsString("employeeId");
                    ServerWebExchange trustedExchange = exchange.mutate()
                            .request(request -> {
                                request.headers(headers -> {
                                    headers.remove(TENANT_HEADER);
                                    headers.remove(EMPLOYEE_HEADER);
                                });
                                request.header(TENANT_HEADER, companyId);
                                if (employeeId != null && !employeeId.isBlank()) {
                                    request.header(EMPLOYEE_HEADER, employeeId);
                                }
                            })
                            .build();
                    return chain.filter(trustedExchange);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
