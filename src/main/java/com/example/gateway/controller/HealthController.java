package com.example.gateway.controller;

import com.example.gateway.dto.HealthCheckRequest;
import com.example.gateway.dto.HealthResponse;
import com.example.gateway.service.FileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final FileGenerator fileGenerator;

    public HealthController(FileGenerator fileGenerator) {
        this.fileGenerator = fileGenerator;
    }

    @PostMapping("/health")
    public HealthResponse checkHealth(@RequestBody HealthCheckRequest request, ServerHttpRequest serverHttpRequest) {
        String correlationId = serverHttpRequest.getHeaders().getFirst(CORRELATION_ID_HEADER);
        log.info("checkHealth received reportType={} includeDetails={} correlationId={}",
                request.reportType(), request.includeDetails(), correlationId);

        log.debug("Invoking FileGenerator with payload correlationId={}", correlationId);
        Path generatedPath = fileGenerator.generate(request);
        log.info("File generated at path={} correlationId={}", generatedPath.toAbsolutePath(), correlationId);

        HealthResponse response = new HealthResponse("sboot-security-base-api-gateway",
                "UP",
                Instant.now(),
                request.reportType());

        log.info("checkHealth responding status={} requestedType={} correlationId={} file={}",
                response.status(), response.requestedType(), correlationId, generatedPath.getFileName());

        return response;
    }
}