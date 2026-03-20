package com.example.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";


    @ExceptionHandler(UncheckedIOException.class)
    public ProblemDetail handleUncheckedIO(UncheckedIOException ex) {
        String correlationId = MDC.get(CORRELATION_ID_HEADER);
        log.error("Erro de I/O não verificada correlationId={}", correlationId, ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de I/O durante o processamento");
        pd.setTitle("I/O error");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        String correlationId = MDC.get(CORRELATION_ID_HEADER);
        log.error("Erro inesperado correlationId={}", correlationId, ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
        pd.setTitle("Internal server error");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
