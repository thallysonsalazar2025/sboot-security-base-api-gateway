package com.example.gateway.service;

import com.example.gateway.client.PayrollQueryClient;
import com.example.gateway.dto.PayrollRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceImplTest {

    @Mock
    private PayrollQueryClient payrollQueryClient;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    @Test
    void getPayroll_ShouldCallClientWithCorrectRequest() {
        when(payrollQueryClient.getPayroll(any(PayrollRequest.class)))
                .thenReturn(Mono.just(ResponseEntity.ok().build()));

        StepVerifier.create(payrollService.getPayroll("comp1", "emp1", 2023, 10))
                .expectNextCount(1)
                .verifyComplete();

        // Verify that the request object created matches expected values
        verify(payrollQueryClient).getPayroll(new PayrollRequest("comp1", "emp1", 2023, 10));
    }
}
