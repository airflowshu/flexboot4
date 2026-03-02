package com.yunlbd.flexboot4.aigateway.web;

import com.yunlbd.flexboot4.aigateway.security.ApiKeySnapshotCache;
import com.yunlbd.flexboot4.common.ApiResult;
import io.jsonwebtoken.Claims;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AiDiagController {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApiKeySnapshotCache snapshotCache;

    public AiDiagController(ReactiveStringRedisTemplate redisTemplate, ApiKeySnapshotCache snapshotCache) {
        this.redisTemplate = redisTemplate;
        this.snapshotCache = snapshotCache;
    }

    @GetMapping("/api/ai/diag/auth")
    public Mono<ApiResult<Map<String, Object>>> diagAuth(ServerWebExchange exchange) {
        Claims claims = (Claims) exchange.getAttributes().get(Claims.class.getName());
        if (claims == null) {
            return Mono.just(ApiResult.error(401, "未登录或登录已过期"));
        }
        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            return Mono.just(ApiResult.error(401, "无效的用户信息"));
        }

        Mono<String> latestVer = redisTemplate.opsForValue().get("aikey:snapshot:latest").defaultIfEmpty("");
        Mono<String> cacheVer = redisTemplate.opsForValue().get("cache:ver:ai_api_key").defaultIfEmpty("");
        Mono<String> apiKeyMono = redisTemplate.opsForValue().get("aikey:user:" + userId).defaultIfEmpty("");

        return Mono.zip(latestVer, cacheVer, apiKeyMono)
                .flatMap(t -> {
                    String latest = t.getT1();
                    String ver = t.getT2();
                    String apiKey = t.getT3();

                    Map<String, Object> out = new HashMap<>();
                    out.put("userId", userId);
                    out.put("snapshotLatest", latest);
                    out.put("tableVersion", ver);
                    out.put("apiKeyPresent", apiKey != null && !apiKey.isBlank());

                    if (apiKey == null || apiKey.isBlank()) {
                        return snapshotCache.activeRulesForUser(userId)
                                .map(rules -> {
                                    out.put("activeRulesCount", rules.size());
                                    return ApiResult.success(out);
                                });
                    }

                    String apiKeyTrim = apiKey.trim();
                    String apiKeyHash = sha256Hex(apiKeyTrim);
                    out.put("apiKeyHash", apiKeyHash);

                    Mono<String> mappingMono = redisTemplate.opsForHash().get("aikey:mapping", apiKeyTrim)
                            .map(String::valueOf)
                            .defaultIfEmpty("");

                    return Mono.zip(mappingMono, snapshotCache.activeRulesForUser(userId).defaultIfEmpty(java.util.List.of()))
                            .map(t2 -> {
                                String mapping = t2.getT1();
                                var rules = t2.getT2();
                                out.put("mappingValue", mapping);
                                out.put("mappingMatch", mappingMatch(mapping, userId));
                                out.put("activeRulesCount", rules.size());
                                out.put("ruleMatch", rules.stream().anyMatch(r -> r != null && apiKeyHash.equals(r.keyHash())));
                                return ApiResult.success(out);
                            });
                });
    }

    private static boolean mappingMatch(String mappingValue, String userId) {
        if (mappingValue == null || mappingValue.isBlank() || userId == null || userId.isBlank()) {
            return false;
        }
        int idx = mappingValue.indexOf(':');
        if (idx <= 0) {
            return false;
        }
        String uid = mappingValue.substring(0, idx);
        String status = mappingValue.substring(idx + 1);
        return userId.equals(uid) && "1".equals(status);
    }

    private static String sha256Hex(String raw) {
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
}

