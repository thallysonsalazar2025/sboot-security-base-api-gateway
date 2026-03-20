package com.example.gateway.dto;

public record PayrollRequest(
        String companyId,
        String employeeId,
        int periodYear,
        int periodMonth
) {
}
