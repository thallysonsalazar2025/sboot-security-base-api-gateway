package com.example.gateway.controller;

import com.example.gateway.service.PayrollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(PayrollController.class)
public class PayrollControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private PayrollService payrollService;

    // Necessário mockar o JwtDecoder pois o WebFluxTest carrega a configuração de segurança
    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void shouldReturnPayrollWhenAuthenticated() {
        when(payrollService.getPayroll(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(Mono.just(ResponseEntity.ok("ok")));

        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("companyId", "123").claim("employeeId", "456")))
                .get().uri("/api/v1/payroll?year=2023&month=1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void shouldReturnForbiddenWhenClaimsMissing() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(jwt -> jwt.claim("sub", "user"))) // Missing companyId/employeeId
                .get().uri("/api/v1/payroll?year=2023&month=1")
                .exchange()
                .expectStatus().isForbidden();
    }
}
