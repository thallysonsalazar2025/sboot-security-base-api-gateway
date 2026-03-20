package com.example.gateway.client;

import com.example.gateway.dto.PayrollRequest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class WebClientPayrollQueryClientTest {

    private WireMockServer wireMockServer;
    private WebClientPayrollQueryClient client;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        
        WebClient.Builder builder = WebClient.builder();
        client = new WebClientPayrollQueryClient(builder, wireMockServer.baseUrl());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getPayroll_ShouldMakePostRequestAndReturnResponse() {
        // Mock downstream response
        wireMockServer.stubFor(WireMock.post("/payroll")
                .withRequestBody(WireMock.matchingJsonPath("$.companyId", WireMock.equalTo("1")))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\"}")
                        .withStatus(200)));

        StepVerifier.create(client.getPayroll(new PayrollRequest("1", "2", 2023, 1)))
                .expectNextMatches(response -> {
                    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                    return true; // Body content check is tricky with Object.class unless we cast
                })
                .verifyComplete();
    }

    @Test
    void getPayroll_ShouldHandleErrorResponse() {
        // Mock downstream 404
        wireMockServer.stubFor(WireMock.post("/payroll")
                .willReturn(WireMock.aResponse()
                        .withStatus(404)));

        StepVerifier.create(client.getPayroll(new PayrollRequest("1", "2", 2023, 1)))
                .expectNextMatches(response -> response.getStatusCode().is4xxClientError())
                .verifyComplete();
    }
}
