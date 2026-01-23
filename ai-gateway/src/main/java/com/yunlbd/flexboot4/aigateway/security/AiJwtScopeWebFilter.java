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
import org.springframework.http.HttpHeaders;
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
public class AiJwtScopeWebFilter implements WebFilter {

    private final ObjectMapper objectMapper;

    public AiJwtScopeWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        //TODO 目前只结构了`JWT 里有 claim: ai`，访问ai接口时应同时还带有`X-AI-API-KEY: sk-xxxx`,还未实现
        // 从 Authorization 请求头获取ai-api-key
        String aiAuth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (aiAuth != null && aiAuth.startsWith("X-AI-API-KEY ")) {
            // 这里的 API Key 只是先验证，真正调用 AI 服务时通过 APISIX 进一步限流/鉴权。
        } else {

        }
        return chain.filter(exchange).contextWrite(ctx -> ctx.put(Claims.class, claims).put(ServerWebExchange.class, exchange));
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
        ApiResult<Void> error = ApiResult.error(status.value(), message);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json;
        try {
            json = objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException e) {
            json = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}";
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
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
