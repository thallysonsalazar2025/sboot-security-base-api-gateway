package com.example.gateway.dto;

public record HealthCheckRequest(String reportType, boolean includeDetails) {
}