package com.crypto_shield.api_gateway.components;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Autowired
    private RedisTokenRepository redisTokenRepository;  // dùng lại y nguyên từ code cũ

    private final String[] PUBLIC_ENDPOINTS = {"/api/auth/login", "/api/auth/register", "/api/auth/refresh"};


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (Arrays.stream(PUBLIC_ENDPOINTS)
                .anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }
        System.out.println(">>> JwtAuthFilter running for: " + path);
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }

        String token = authHeader.substring(7);

        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
                return;
            }

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expiryTime == null || !expiryTime.after(new Date())) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
                return;
            }

            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            if (jti != null && redisTokenRepository.existsById(jti)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
                return;
            }
            String email = signedJWT.getJWTClaimsSet().getSubject();

            System.out.println(">>> verify result: " + signedJWT.verify(verifier));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Forward thông tin user xuống service qua header — dùng wrapper để thêm header
            HttpServletRequest mutatedRequest = new HeaderMapRequestWrapper(request,
                    Map.of("X-User-Email", signedJWT.getJWTClaimsSet().getSubject()));

            filterChain.doFilter(mutatedRequest, response);

        } catch (ParseException | JOSEException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Malformed token");
        }
    }
}
