package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayRouteConfig {

    @Value("${holerite.service.url:http://holerite-service:8080}")
    private String holeriteServiceUrl;

    @Value("${user.service.url:http://user-service:8080}")
    private String userServiceUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("holerite-generate", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/holerites/generate")
                        .uri(holeriteServiceUrl))
                .route("holerite-by-id", route -> route
                        .method(HttpMethod.GET)
                        .and()
                        .path("/holerites/{id}")
                        .uri(holeriteServiceUrl))
                .route("user-by-id", route -> route
                        .method(HttpMethod.GET)
                        .and()
                        .path("/users/{id}")
                        .uri(userServiceUrl))
                .build();
    }
}
