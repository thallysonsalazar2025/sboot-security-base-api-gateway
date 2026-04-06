package com.example.gateway.controller;

import com.example.gateway.service.PayrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PayrollController.class);

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

        LOGGER.info("Payroll request received companyId={} employeeId={} period={}-{}",
                companyId,
                employeeId,
                year,
                month);

        if (companyId == null || employeeId == null) {
            LOGGER.warn("Payroll request rejected: required claims missing");
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User context missing required claims"));
        }

        return payrollService.getPayroll(companyId, employeeId, year, month)
                .doOnSuccess(response -> LOGGER.info("Payroll request completed status={} period={}-{}",
                        response.getStatusCode(),
                        year,
                        month));
    }
}
