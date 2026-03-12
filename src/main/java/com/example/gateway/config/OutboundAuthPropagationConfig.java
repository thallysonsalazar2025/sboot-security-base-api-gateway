package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
public class OutboundAuthPropagationConfig {

    private static final Logger log = LoggerFactory.getLogger(OutboundAuthPropagationConfig.class);

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(authorizationPropagationInterceptor());
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor authorizationPropagationInterceptor() {
        return (request, body, execution) -> {
            String bearer = resolveBearerToken();
            if (bearer != null && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
                log.debug("Propagated Authorization header to downstream request: {}", request.getURI());
            }
            return execution.execute(request, body);
        };
    }

    /**
     * Tenta extrair o token do SecurityContext reativo (WebFlux).
     * Fallback: tenta obter do MDC caso tenha sido previamente colocado.
     */
    private String resolveBearerToken() {
        try {
            return ReactiveSecurityContextHolder.getContext()
                    .map(SecurityContext::getAuthentication)
                    .flatMap(auth -> {
                        if (auth instanceof JwtAuthenticationToken jwtAuth) {
                            return Mono.just(jwtAuth.getToken().getTokenValue());
                        }
                        Object credentials = auth.getCredentials();
                        if (credentials instanceof String s && !s.isBlank()) {
                            return Mono.just(s);
                        }
                        return Mono.empty();
                    })
                    .blockOptional(Duration.ofMillis(50))
                    .orElseGet(() -> {
                        String authFromMdc = MDC.get(HttpHeaders.AUTHORIZATION);
                        if (authFromMdc != null && authFromMdc.startsWith("Bearer ")) {
                            return authFromMdc.substring(7);
                        }
                        return null;
                    });
        } catch (Exception e) {
            log.debug("Could not resolve bearer token from reactive security context: {}", e.getMessage());
            return null;
        }
    }
}
