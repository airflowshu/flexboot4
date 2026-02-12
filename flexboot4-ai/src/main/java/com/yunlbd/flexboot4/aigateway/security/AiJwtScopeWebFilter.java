package com.yunlbd.flexboot4.aigateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.auth.jwt.JwtClaimKeys;
import com.yunlbd.flexboot4.auth.jwt.JwtScopes;
import com.yunlbd.flexboot4.common.ApiResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@Component
@Order(0)
public class AiJwtScopeWebFilter implements WebFilter {

    private final ObjectMapper objectMapper;
    private final ApiKeySnapshotCache apiKeySnapshotCache;

    public AiJwtScopeWebFilter(ObjectMapper objectMapper, ApiKeySnapshotCache apiKeySnapshotCache) {
        this.objectMapper = objectMapper;
        this.apiKeySnapshotCache = apiKeySnapshotCache;
    }

    @Value("${jwt.secret:thisIsASecretKeyThatIsLongEnoughForHmacSha256SecurityRequirement}")
    private String secret;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/ai/health")) {
            return chain.filter(exchange);
        }

        String token = null;
        // 从 Cookie 中获取jwt access_token
        String cookie = exchange.getRequest().getHeaders().getFirst("Cookie");
        if (cookie != null) {
            token = extractCookieValue(cookie);
        }

        if (token == null) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "无效的访问令牌");
        }

        Object scope = claims.get(JwtClaimKeys.SCOPE);
        if (!hasAiScope(scope)) {
            return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "无AI接口访问权限");
        }

        String userId = claims.getSubject();
        // 校验是否已分配有效的 API Key
        return apiKeySnapshotCache.activeRulesForUser(userId)
                .flatMap(rules -> {
                    if (rules.isEmpty()) {
                        return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, "未开通AI服务或服务已禁用");
                    }
                    exchange.getAttributes().put(Claims.class.getName(), claims);
                    return chain.filter(exchange)
                            .contextWrite(ctx -> ctx.put(Claims.class, claims)
                                    .put(ServerWebExchange.class, exchange));
                });
    }

    private boolean hasAiScope(Object scope) {
        switch (scope) {
            case null -> {
                return false;
            }
            case String s -> {
                return JwtScopes.AI.equals(s);
            }
            case Collection<?> c -> {
                for (Object it : c) {
                    if (JwtScopes.AI.equals(String.valueOf(it))) {
                        return true;
                    }
                }
                return false;
            }
            case Object[] arr -> {
                return hasAiScope(List.of(arr));
            }
            default -> {
            }
        }
        return false;
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        // 统一使用 ApiResult.error 构建响应体
        ApiResult<Void> errorResult = ApiResult.error(status.value(), message);
        
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResult);
        } catch (JsonProcessingException e) {
            String fallback = String.format("{\"code\":%d,\"message\":\"%s\",\"error\":\"error\"}", status.value(), message);
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }
        
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(bytes)));
    }

    private String extractCookieValue(String cookieHeader) {
        if (cookieHeader == null) {
            return null;
        }
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            int eqIndex = trimmed.indexOf('=');
            if (eqIndex > 0) {
                String name = trimmed.substring(0, eqIndex);
                String value = trimmed.substring(eqIndex + 1);
                if ("access_token".equals(name)) {
                    return value;
                }
            }
        }
        return null;
    }
}
