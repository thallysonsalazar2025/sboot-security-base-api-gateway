package com.example.gateway.service;

import com.example.gateway.client.PayrollQueryClient;
import com.example.gateway.dto.PayrollRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PayrollServiceImpl implements PayrollService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayrollServiceImpl.class);

    private final PayrollQueryClient payrollQueryClient;

    public PayrollServiceImpl(PayrollQueryClient payrollQueryClient) {
        this.payrollQueryClient = payrollQueryClient;
    }

    @Override
    public Mono<ResponseEntity<Object>> getPayroll(String companyId, String employeeId, int year, int month) {
        PayrollRequest request = new PayrollRequest(companyId, employeeId, year, month);
        LOGGER.debug("Forwarding request to payroll-query-service companyId={} employeeId={} period={}-{}",
                companyId,
                employeeId,
                year,
                month);
        return payrollQueryClient.getPayroll(request);
    }
}
