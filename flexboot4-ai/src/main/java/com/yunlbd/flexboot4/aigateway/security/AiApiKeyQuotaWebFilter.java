package com.yunlbd.flexboot4.aigateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.aigateway.service.AiQuotaService;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.common.ApiResult;
import io.jsonwebtoken.Claims;
import org.jspecify.annotations.NullMarked;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
@Order(1)
public class AiApiKeyQuotaWebFilter implements WebFilter {

    private static final String AI_API_KEY_MAPPING = "aikey:mapping";
    private static final String AI_API_KEY_USER_PREFIX = "aikey:user:";

    public static final String ATTR_API_KEY_RULE = ApiKeyRule.class.getName();
    public static final String ATTR_API_KEY_RAW = "ai.apiKeyRaw";
    public static final String ATTR_QUOTA_EXHAUSTED = "ai.quota.exhausted";

    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiKeySnapshotCache apiKeySnapshotCache;
    private final AiQuotaService quotaService;

    public AiApiKeyQuotaWebFilter(ObjectMapper objectMapper,
                                  ReactiveStringRedisTemplate redisTemplate,
                                  ApiKeySnapshotCache apiKeySnapshotCache,
                                  AiQuotaService quotaService) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.apiKeySnapshotCache = apiKeySnapshotCache;
        this.quotaService = quotaService;
    }

    @NullMarked
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/ai/health") || path.startsWith("/api/ai/diag")) {
            return chain.filter(exchange);
        }

        Claims claims = (Claims) exchange.getAttributes().get(Claims.class.getName());
        if (claims == null) {
            return chain.filter(exchange);
        }

        String userId = claims.getSubject();

        Mono<String> authorizedApiKeyMono = resolveAuthorizedApiKey(exchange, userId);

        Mono<Boolean> authMono = authorizedApiKeyMono
                .flatMap(apiKey -> {
                    String hash = sha256Hex(apiKey);
                    return apiKeySnapshotCache.activeRulesForUser(userId)
                            .flatMap(rules -> {
                                ApiKeyRule rule = selectRule(rules, hash);
                                if (rule == null) {
                                    return Mono.error(new RuntimeException("API Key 未授权"));
                                }
                                exchange.getAttributes().put(ATTR_API_KEY_RULE, rule);
                                exchange.getAttributes().put(ATTR_API_KEY_RAW, apiKey);
                                return quotaService.decide(rule)
                                        .map(decision -> {
                                            exchange.getAttributes().put(ATTR_QUOTA_EXHAUSTED, decision.exhausted());
                                            return true;
                                        });
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("API Key 无效或已禁用")));

        return authMono
                .flatMap(ok -> chain.filter(exchange))
                .onErrorResume(e -> {
                    // 如果是认证过程中的 RuntimeException，返回 403
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("API Key") || msg.contains("配额"))) {
                        return writeErrorResponse(exchange, HttpStatus.FORBIDDEN, msg);
                    }
                    // 其他下游错误（如 LLM 超时、Redis 业务超时等）直接抛出，由全局异常处理器或日志切面处理
                    return Mono.error(e);
                });
    }

    private Mono<String> resolveAuthorizedApiKey(ServerWebExchange exchange, String userId) {
        Mono<String> headerApiKey = Mono.fromSupplier(() -> exchange.getRequest().getHeaders().getFirst("X-AI-API-KEY"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !"undefined".equalsIgnoreCase(s))
                .filter(s -> !"null".equalsIgnoreCase(s));

        Mono<String> redisApiKey = resolveApiKeyFromUserCache(userId);

        return Flux.concat(headerApiKey, redisApiKey)
                .concatMap(apiKey -> validateApiKeyOwner(apiKey, userId))
                .next();
    }

    private Mono<String> resolveApiKeyFromUserCache(String userId) {
        if (userId == null || userId.isBlank()) {
            return Mono.empty();
        }
        return redisTemplate.opsForValue()
                .get(AI_API_KEY_USER_PREFIX + userId)
                .map(String::trim)
                .filter(s -> !s.isBlank());
    }

    private Mono<String> validateApiKeyOwner(String apiKey, String userId) {
        return redisTemplate.opsForHash()
                .get(AI_API_KEY_MAPPING, apiKey)
                .map(val -> {
                    if (val == null) {
                        return false;
                    }
                    String s = String.valueOf(val);
                    if (s.isBlank()) {
                        return false;
                    }
                    int idx = s.indexOf(':');
                    if (idx <= 0) {
                        return false;
                    }
                    String uid = s.substring(0, idx);
                    String status = s.substring(idx + 1);
                    return userId != null && userId.equals(uid) && "1".equals(status);
                })
                .filter(Boolean::booleanValue)
                .map(ok -> apiKey);
    }

    private ApiKeyRule selectRule(List<ApiKeyRule> rules, String keyHash) {
        if (rules == null || rules.isEmpty() || keyHash == null || keyHash.isBlank()) {
            return null;
        }
        for (ApiKeyRule r : rules) {
            if (r != null && keyHash.equals(r.keyHash())) {
                return r;
            }
        }
        return null;
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
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
}
