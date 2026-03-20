package com.example.gateway.client;

import com.example.gateway.dto.PayrollRequest;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface PayrollQueryClient {
    Mono<ResponseEntity<Object>> getPayroll(PayrollRequest request);
}
