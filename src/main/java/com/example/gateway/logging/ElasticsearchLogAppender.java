package com.example.gateway.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElasticsearchLogAppender extends AppenderBase<ILoggingEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "elasticsearch-log-appender");
        thread.setDaemon(true);
        return thread;
    });

    private String endpoint;
    private boolean enabled;
    private String index = "sboot-security-base-api-gateway-logs";
    private String apiKey;

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled || endpoint == null || endpoint.isBlank()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("@timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        payload.put("service", Map.of("name", "sboot-security-base-api-gateway"));
        payload.put("log", Map.of("level", event.getLevel().toString(), "logger", event.getLoggerName()));
        payload.put("message", event.getFormattedMessage());
        payload.put("thread", event.getThreadName());
        payload.put("correlationId", event.getMDCPropertyMap().getOrDefault("correlationId", "N/A"));

        if (event.getThrowableProxy() != null) {
            payload.put("error", Map.of("message", event.getThrowableProxy().getMessage()));
        }

        executorService.submit(() -> postToElasticsearch(payload));
    }

    private void postToElasticsearch(Map<String, Object> payload) {
        try {
            String requestBody = OBJECT_MAPPER.writeValueAsString(payload);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/%s/_doc", endpoint, index)))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

            if (apiKey != null && !apiKey.isBlank()) {
                requestBuilder.header("Authorization", "ApiKey " + apiKey);
            }

            httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception exception) {
            addWarn("Failed to publish log to Elasticsearch: " + exception.getMessage());
        }
    }

    @Override
    public void stop() {
        super.stop();
        executorService.shutdown();
    }
}
