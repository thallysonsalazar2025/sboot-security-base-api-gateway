package com.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("holerite-generate", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/holerites/generate")
                        .uri("http://holerite-service:8080"))
                .route("holerite-by-id", route -> route
                        .method(HttpMethod.GET)
                        .and()
                        .path("/holerites/{id}")
                        .uri("http://holerite-service:8080"))
                .build();
    }
}
