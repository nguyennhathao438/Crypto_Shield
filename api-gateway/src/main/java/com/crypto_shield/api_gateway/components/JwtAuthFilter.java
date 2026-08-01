package com.crypto_shield.api_gateway.components;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
public class JwtAuthFilter implements WebFilter {

    private final String signerKey;
    private final RedisTokenRepository redisTokenRepository;

    private final String[] PUBLIC_ENDPOINTS = {"/api/auth/login", "/api/auth/register", "/api/auth/refresh"};

    public JwtAuthFilter(String signerKey, RedisTokenRepository redisTokenRepository) {
        this.signerKey = signerKey;
        this.redisTokenRepository = redisTokenRepository;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain)  {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (Arrays.stream(PUBLIC_ENDPOINTS)
                .anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        System.out.println(">>> JwtAuthFilter running for: " + path);
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing token");
        }

        String token = authHeader.substring(7);

        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                return unauthorized(exchange, "Invalid signature");
            }

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiryTime == null || !expiryTime.after(new Date())) {
                return unauthorized(exchange, "Token expired");
            }

            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            String email = signedJWT.getJWTClaimsSet().getSubject();

            // Bước check Redis là I/O — nên chuyển sang reactive để không block event loop
            Mono<Boolean> revokedCheck = (jti != null)
                    ? checkRevoked(jti)
                    : Mono.just(false);

            return revokedCheck.flatMap(isRevoked -> {
                if (isRevoked) {
                    return unauthorized(exchange, "Token revoked");
                }

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Email", email)
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                return chain.filter(mutatedExchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            });

        } catch (ParseException | JOSEException e) {
            return unauthorized(exchange, "Malformed token");
        }
    }
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String body = String.format("{\"error\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
    private Mono<Boolean> checkRevoked(String jti) {
        // TODO: nếu RedisTokenRepository.existsById() là blocking (Spring Data Redis thường
        // thì đây LÀ blocking call, cần bọc lại để tránh chặn Netty event loop:
        return Mono.fromCallable(() -> redisTokenRepository.existsById(jti))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}
