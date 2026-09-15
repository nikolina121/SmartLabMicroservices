package org.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Value("${smartlab.jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Claims claims = parse(auth);
        if (claims == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Nedostaje ili nije validan JWT token.");
        }

        String role = String.valueOf(claims.get("role"));
        if (requiresEngineer(path, exchange.getRequest().getMethod())
                && !"ENGINEER".equals(role) && !"ADMIN".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Ova akcija zahteva ulogu ENGINEER ili ADMIN.");
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(claims.get("userId")))
                .header("X-User-Role", role)
                .header("X-User-Roles", role)
                .header("X-Username", claims.getSubject())
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isPublic(String path) {
        return path.startsWith("/auth/")
                || path.contains("/actuator")
                || path.contains("/swagger")
                || path.contains("/v3/api-docs");
    }

    private boolean requiresEngineer(String path, HttpMethod method) {
        boolean writeInventory = method != null && method != HttpMethod.GET && path.startsWith("/inventory");
        return writeInventory
                || path.contains("/admin/")
                || path.contains("/reports/")
                || path.contains("/penalties/")
                || path.endsWith("/approve")
                || path.endsWith("/reject")
                || path.endsWith("/assign")
                || path.endsWith("/pickup")
                || path.endsWith("/return")
                || path.contains("/process-overdues")
                || path.contains("/process-no-shows");
    }

    private Claims parse(String auth) {
        try {
            if (auth == null || !auth.startsWith("Bearer ")) {
                return null;
            }
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(auth.substring(7))
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
