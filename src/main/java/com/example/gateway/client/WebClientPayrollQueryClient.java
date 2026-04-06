package com.example.gateway.client;

import com.example.gateway.dto.PayrollRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebClientPayrollQueryClient implements PayrollQueryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebClientPayrollQueryClient.class);

    private final WebClient webClient;

    public WebClientPayrollQueryClient(WebClient.Builder webClientBuilder,
                                       @Value("${services.payroll-query.url}") String serviceUrl) {
        this.webClient = webClientBuilder.baseUrl(serviceUrl).build();
    }

    @Override
    public Mono<ResponseEntity<Object>> getPayroll(PayrollRequest request) {
        return webClient.post()
                .uri("/payroll")
                .bodyValue(request)
                .exchangeToMono(response -> response.toEntity(Object.class))
                .doOnSuccess(response -> LOGGER.info("Downstream payroll-query-service status={}", response.getStatusCode()))
                .doOnError(error -> LOGGER.error("Downstream payroll-query-service call failed", error));
    }
}
