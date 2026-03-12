package com.example.gateway;

import com.example.gateway.dto.HealthCheckRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "holerite.service.url=http://localhost:${wiremock.server.port}",
        "user.service.url=http://localhost:${wiremock.server.port}"
})
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 0)
class HealthControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @WithMockUser
    void healthEndpoint_ShouldReturnUpStatus() {
        // Cria o objeto necessário para o request
        HealthCheckRequest request = new HealthCheckRequest("FULL_REPORT", true);

        webTestClient.post()
                .uri("/api/v1/health")
                .bodyValue(request) // Injeta o objeto no corpo da requisição
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(this::logOnError)
                .jsonPath("$.service").isEqualTo("sboot-security-base-api-gateway")
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.timestamp").isNotEmpty();
    }



    private void logOnError(org.springframework.test.web.reactive.server.EntityExchangeResult<byte[]> response) {
        if (response.getStatus().isError()) {
            byte[] body = response.getResponseBody();
            String bodyStr = (body != null) ? new String(body) : "empty body";
            org.slf4j.LoggerFactory.getLogger(HealthControllerIntegrationTest.class)
                    .error("Request failed with status {} and body: {}", response.getStatus(), bodyStr);
        }
    }
}
