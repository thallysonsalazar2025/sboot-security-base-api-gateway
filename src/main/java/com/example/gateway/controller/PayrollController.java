package com.example.gateway.controller;

import com.example.gateway.service.PayrollService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/payroll")
    public Mono<ResponseEntity<Object>> getPayroll(
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String companyId = jwt.getClaimAsString("companyId");
        String employeeId = jwt.getClaimAsString("employeeId");

        if (companyId == null || employeeId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User context missing required claims"));
        }

        return payrollService.getPayroll(companyId, employeeId, year, month);
    }
}
