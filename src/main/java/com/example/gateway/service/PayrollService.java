package com.example.gateway.service;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface PayrollService {
    Mono<ResponseEntity<Object>> getPayroll(String companyId, String employeeId, int year, int month);
}
