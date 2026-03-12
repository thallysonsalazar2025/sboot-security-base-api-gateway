package com.example.gateway.dto;

import java.time.Instant;

public record HealthResponse(String service, String status, Instant timestamp, String requestedType) {}