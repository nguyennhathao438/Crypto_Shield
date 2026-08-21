package com.crypto_shield.api_gateway.components;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

@Configuration
public class RateLimitConfig {
    @Value("${jwt.signerKey}")
    private String signerKey;
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    SignedJWT signedJWT = SignedJWT.parse(token);

                    JWSVerifier verifier = new MACVerifier(signerKey.getBytes(StandardCharsets.UTF_8));

                    if (signedJWT.verify(verifier)) {
                        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

                        // kiểm tra hết hạn
                        Date expiration = claims.getExpirationTime();
                        if (expiration != null && expiration.before(new Date())) {
                            return getIpAddress(exchange);
                        }

                        String userId = claims.getSubject();
                        return Mono.just(userId != null ? userId : "anonymous");
                    }
                } catch (ParseException | JOSEException e) {
                    return getIpAddress(exchange);
                }
            }
            return getIpAddress(exchange);
        };
    }
        private Mono<String> getIpAddress(ServerWebExchange exchange) {
            return Mono.just(
                    Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                            .getAddress().getHostAddress()
            );
        }
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress().getHostAddress()
        );
    }
}
