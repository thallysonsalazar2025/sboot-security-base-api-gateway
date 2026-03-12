package com.example.gateway.service;

import com.example.gateway.dto.HealthCheckRequest;
import com.example.gateway.exception.FileGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Component
public class FileGenerator {

    private static final Logger log = LoggerFactory.getLogger(FileGenerator.class);

    /**
     * Gera um arquivo simples contendo informações do HealthCheckRequest.
     * O arquivo é gravado no diretório temporário do sistema em "reports".
     *
     * @param request payload recebido no endpoint de health
     * @return o caminho do arquivo gerado
     */
    public Path generate(HealthCheckRequest request) {
        log.info("Starting file generation reportType={} includeDetails={}",
                request.reportType(), request.includeDetails());
        try {
            Path outDir = Paths.get(System.getProperty("java.io.tmpdir"), "reports");
            log.debug("Ensuring output directory exists: {}", outDir.toAbsolutePath());
            Files.createDirectories(outDir);

            String fileName = "health-" + Instant.now().toEpochMilli() + ".txt";
            Path outFile = outDir.resolve(fileName);
            log.debug("Target file resolved: {}", outFile.toAbsolutePath());

            String content = """
                    service=sboot-security-base-api-gateway
                    status=UP
                    timestamp=%s
                    reportType=%s
                    includeDetails=%s
                    """.formatted(
                    Instant.now(),
                    request.reportType(),
                    request.includeDetails()
            );

            Files.writeString(
                    outFile,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            log.info("File successfully generated: {}", outFile.toAbsolutePath());
            return outFile;
        } catch (IOException e) {
            log.error("Error generating file: {}", e.getMessage(), e);
            throw new FileGenerationException("Falha ao gerar arquivo de health check", e);
        }
    }
}
