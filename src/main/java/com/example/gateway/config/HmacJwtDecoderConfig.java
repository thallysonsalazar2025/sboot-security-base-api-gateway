package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

@Configuration
public class HmacJwtDecoderConfig {

    /**
     * Decoder para tokens HMAC HS512.
     * Use a env var JWT_HS512_SECRET para definir o segredo em produção.
     */
    @Bean
    @Primary
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${security.jwt.hmac-secret}") String secret
    ) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}
