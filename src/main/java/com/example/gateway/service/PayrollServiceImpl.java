package com.example.gateway.service;

import com.example.gateway.client.PayrollQueryClient;
import com.example.gateway.dto.PayrollRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PayrollServiceImpl implements PayrollService {

    private final PayrollQueryClient payrollQueryClient;

    public PayrollServiceImpl(PayrollQueryClient payrollQueryClient) {
        this.payrollQueryClient = payrollQueryClient;
    }

    @Override
    public Mono<ResponseEntity<Object>> getPayroll(String companyId, String employeeId, int year, int month) {
        PayrollRequest request = new PayrollRequest(companyId, employeeId, year, month);
        return payrollQueryClient.getPayroll(request);
    }
}
