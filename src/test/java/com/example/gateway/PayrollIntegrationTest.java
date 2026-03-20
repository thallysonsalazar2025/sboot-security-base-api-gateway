package com.example.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"services.payroll-query.url=http://localhost:${wiremock.server.port}"})
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
public class PayrollIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldProxyPayrollRequestSuccessfully() {
        // Mock downstream service
        WireMock.stubFor(WireMock.post("/payroll")
                .withRequestBody(WireMock.matchingJsonPath("$.companyId", WireMock.equalTo("123")))
                .withRequestBody(WireMock.matchingJsonPath("$.employeeId", WireMock.equalTo("456")))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":\"payroll-data\"}")
                        .withStatus(200)));

        // Call Gateway endpoint
        webTestClient.mutateWith(mockJwt()
                .jwt(jwt -> jwt.claim("companyId", "123").claim("employeeId", "456")))
                .get().uri("/api/v1/payroll?year=2023&month=5")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data").isEqualTo("payroll-data");
    }

    @Test
    void shouldReturnForbiddenWhenMissingClaims() {
        webTestClient.mutateWith(mockJwt()
                .jwt(jwt -> jwt.claim("sub", "user"))) // Missing companyId/employeeId
                .get().uri("/api/v1/payroll?year=2023&month=5")
                .exchange()
                .expectStatus().isForbidden();
    }
}
