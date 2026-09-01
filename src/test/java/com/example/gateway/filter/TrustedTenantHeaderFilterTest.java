package com.example.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedTenantHeaderFilterTest {

    private final TrustedTenantHeaderFilter filter = new TrustedTenantHeaderFilter();

    @Test
    void replacesSpoofedHeadersWithAuthenticatedClaims() {
        ServerWebExchange exchange = exchange("/api/time-clock/events/sync", "tenant-a", "employee-a");
        AtomicReference<String> tenant = new AtomicReference<>();
        AtomicReference<String> employee = new AtomicReference<>();
        WebFilterChain chain = current -> {
            tenant.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.TENANT_HEADER));
            employee.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.EMPLOYEE_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(tenant.get()).isEqualTo("tenant-a");
        assertThat(employee.get()).isEqualTo("employee-a");
    }

    @Test
    void replacesSpoofedHeadersOnAdjustmentRoute() {
        ServerWebExchange exchange = exchange("/api/time-clock/adjustments/123/decision", "tenant-a", "employee-a");
        AtomicReference<String> tenant = new AtomicReference<>();
        WebFilterChain chain = current -> {
            tenant.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.TENANT_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(tenant.get()).isEqualTo("tenant-a");
    }

    @Test
    void replacesSpoofedHeadersOnTimesheetRoute() {
        ServerWebExchange exchange = exchange("/api/time-clock/timesheets/emp-1", "tenant-a", "employee-a");
        AtomicReference<String> tenant = new AtomicReference<>();
        WebFilterChain chain = current -> {
            tenant.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.TENANT_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(tenant.get()).isEqualTo("tenant-a");
    }

    @Test
    void encodedPointPathCannotBypassTrustedTenantReplacement() {
        ServerWebExchange exchange = exchange("/api/time-clock/events/%73ync", "tenant-a", "employee-a");
        AtomicReference<String> tenant = new AtomicReference<>();
        WebFilterChain chain = current -> {
            tenant.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.TENANT_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(tenant.get()).isEqualTo("tenant-a");
    }

    @Test
    void rejectsPointApiWhenCompanyClaimIsMissing() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.post("/api/time-clock/adjustments/123/decision").build())
                .mutate()
                .principal(Mono.just(auth(null, "employee-a")))
                .build();

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void leavesUnrelatedApiRoutesUntouched() {
        ServerWebExchange exchange = exchange("/api/payroll/query", "tenant-a", "employee-a");
        AtomicReference<String> tenant = new AtomicReference<>();
        WebFilterChain chain = current -> {
            tenant.set(current.getRequest().getHeaders().getFirst(TrustedTenantHeaderFilter.TENANT_HEADER));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(tenant.get()).isEqualTo("tenant-b");
    }

    private ServerWebExchange exchange(String path, String companyId, String employeeId) {
        return MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.post(path)
                        .header(TrustedTenantHeaderFilter.TENANT_HEADER, "tenant-b")
                        .header(TrustedTenantHeaderFilter.EMPLOYEE_HEADER, "employee-b")
                        .build())
                .mutate()
                .principal(Mono.just(auth(companyId, employeeId)))
                .build();
    }

    private JwtAuthenticationToken auth(String companyId, String employeeId) {
        Jwt.Builder builder = Jwt.withTokenValue("test")
                .header("alg", "none")
                .subject("user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
        if (companyId != null) builder.claim("companyId", companyId);
        if (employeeId != null) builder.claim("employeeId", employeeId);
        return new JwtAuthenticationToken(builder.build());
    }
}
