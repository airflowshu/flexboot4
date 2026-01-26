package com.yunlbd.flexboot4.aigateway.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.auth.jwt.JwtClaimKeys;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.operlog.OperLogRecord;
import com.yunlbd.flexboot4.operlog.OperLogSink;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Aspect
@Component
@Order(1) // 确保日志切面在最外层，可以捕获权限等异常
public class AiOperLogAspect {

    private final OperLogSink operLogSink;
    private final ObjectMapper objectMapper;

    public AiOperLogAspect(OperLogSink operLogSink, ObjectMapper objectMapper) {
        this.operLogSink = operLogSink;
        this.objectMapper = objectMapper;
    }

    @Pointcut("@annotation(operLog)")
    public void operLogPointCut(OperLog operLog) {
    }

    @SuppressWarnings("unused")
    @Around(value = "operLogPointCut(operLog)", argNames = "joinPoint,operLog")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        Object result = joinPoint.proceed();
        if (result instanceof Mono<?> mono) {
            return wrapMono(joinPoint, operLog, mono);
        }
        if (result instanceof Flux<?> flux) {
            return wrapFlux(joinPoint, operLog, flux);
        }
        return result;
    }

    private <T> Mono<T> wrapMono(ProceedingJoinPoint joinPoint, OperLog operLog, Mono<T> mono) {
        return Mono.deferContextual(ctx -> {
            ServerWebExchange exchange = ctx.getOrDefault(ServerWebExchange.class, null);
            Claims claims = ctx.getOrDefault(Claims.class, null);
            long startNanos = System.nanoTime();
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> operParam = operLog.isSaveRequestData() ? buildOperParam(joinPoint.getArgs()) : null;
            Map<String, Object> jsonResult = operLog.isSaveResponseData() ? Map.of("note", "ai_response_omitted") : null;

            AtomicBoolean logged = new AtomicBoolean(false);

            return mono
                    .doOnEach(signal -> {
                        if (signal.isOnComplete() || signal.isOnError()) {
                            if (logged.compareAndSet(false, true)) {
                                long costMillis = (System.nanoTime() - startNanos) / 1_000_000;
                                Throwable ex = signal.getThrowable();
                                OperLogRecord record = buildRecord(eventId, joinPoint, operLog, exchange, claims, costMillis, ex, operParam, jsonResult);
                                operLogSink.write(record).toCompletableFuture().join();
                            }
                        }
                    })
                    .doOnCancel(() -> {
                        if (logged.compareAndSet(false, true)) {
                            long costMillis = (System.nanoTime() - startNanos) / 1_000_000;
                            OperLogRecord record = buildRecord(eventId, joinPoint, operLog, exchange, claims, costMillis, null, operParam, jsonResult);
                            operLogSink.write(record);
                        }
                    });
        });
    }

    private <T> Flux<T> wrapFlux(ProceedingJoinPoint joinPoint, OperLog operLog, Flux<T> flux) {
        return Flux.deferContextual(ctx -> {
            ServerWebExchange exchange = ctx.getOrDefault(ServerWebExchange.class, null);
            Claims claims = ctx.getOrDefault(Claims.class, null);
            long startNanos = System.nanoTime();
            String eventId = UUID.randomUUID().toString();
            Map<String, Object> operParam = operLog.isSaveRequestData() ? buildOperParam(joinPoint.getArgs()) : null;
            Map<String, Object> jsonResult = operLog.isSaveResponseData() ? Map.of("note", "ai_stream_response_omitted") : null;

            AtomicBoolean logged = new AtomicBoolean(false);

            return flux
                    .doOnEach(signal -> {
                        if (signal.isOnComplete() || signal.isOnError()) {
                            if (logged.compareAndSet(false, true)) {
                                long costMillis = (System.nanoTime() - startNanos) / 1_000_000;
                                Throwable ex = signal.getThrowable();
                                OperLogRecord record = buildRecord(eventId, joinPoint, operLog, exchange, claims, costMillis, ex, operParam, jsonResult);
                                operLogSink.write(record).toCompletableFuture().join();
                            }
                        }
                    })
                    .doOnCancel(() -> {
                        if (logged.compareAndSet(false, true)) {
                            long costMillis = (System.nanoTime() - startNanos) / 1_000_000;
                            OperLogRecord record = buildRecord(eventId, joinPoint, operLog, exchange, claims, costMillis, null, operParam, jsonResult);
                            operLogSink.write(record);
                        }
                    });
        });
    }

    private OperLogRecord buildRecord(String eventId,
                                     ProceedingJoinPoint joinPoint,
                                     OperLog operLog,
                                     ServerWebExchange exchange,
                                     Claims claims,
                                     long costMillis,
                                     Throwable error,
                                     Map<String, Object> operParam,
                                     Map<String, Object> jsonResult) {
        String title = operLog.title();
        if (title == null || title.isBlank()) {
            title = joinPoint.getTarget().getClass().getSimpleName();
        }

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        String method = className + "." + methodName + "()";

        String requestMethod = exchange != null
                ? exchange.getRequest().getMethod().name()
                : "";
        String operUrl = exchange != null ? exchange.getRequest().getURI().getPath() : "";
        String operIp = exchange != null ? resolveClientIp(exchange) : "";

        Map<String, String> terminal = null;
        if (exchange != null) {
            String ua = exchange.getRequest().getHeaders().getFirst("User-Agent");
            if (ua != null && !ua.isBlank()) {
                terminal = Map.of("userAgent", truncate(ua, 500));
            }
        }

        String operName = claims != null ? Objects.toString(claims.get(JwtClaimKeys.USERNAME), "") : "";
        String operUserId = claims != null ? Objects.toString(claims.getSubject(), "") : "";

        int status = error == null ? 0 : 1;
        String errorMsg = error == null ? "" : truncate(String.valueOf(error.getMessage()), 2000);

        Map<String, Object> extParams = buildExtParams(operParam, costMillis);

        return new OperLogRecord(
                eventId,
                title,
                operLog.businessType().ordinal(),
                operLog.operatorType().ordinal(),
                method,
                requestMethod,
                operUrl,
                operIp,
                terminal,
                operName,
                operUserId,
                null,
                System.currentTimeMillis(),
                costMillis,
                status,
                errorMsg,
                operParam,
                jsonResult,
                extParams
        );
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        String xf = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            int idx = xf.indexOf(',');
            return (idx > 0 ? xf.substring(0, idx) : xf).trim();
        }
        if (exchange.getRequest().getRemoteAddress() == null) {
            return "";
        }
        return Objects.toString(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress(), "");
    }

    private Map<String, Object> buildExtParams(Map<String, Object> operParam, long costMillis) {
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("latencyMs", costMillis);
        if (operParam != null) {
            Object promptHash = operParam.get("promptHash");
            if (promptHash != null) {
                ext.put("promptHash", promptHash);
            }

            Object model = findFirstByKeyHints(operParam, "model", "modelName", "llmModel", "chatModel", "embeddingModel");
            if (model != null) {
                ext.put("model", model);
            }

            Object usage = findFirstByKeyHints(operParam, "tokenUsage", "usage", "tokenusage", "tokens");
            Object promptTokens = findFirstByKeyHints(operParam, "promptTokens", "prompt_tokens");
            Object completionTokens = findFirstByKeyHints(operParam, "completionTokens", "completion_tokens", "outputTokens", "output_tokens");
            Object totalTokens = findFirstByKeyHints(operParam, "totalTokens", "total_tokens");
            Map<String, Object> tokenUsage = new LinkedHashMap<>();
            if (usage instanceof Map<?, ?> m) {
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    tokenUsage.put(String.valueOf(e.getKey()), e.getValue());
                }
            } else if (usage != null) {
                tokenUsage.put("value", usage);
            }
            if (promptTokens != null) {
                tokenUsage.put("promptTokens", promptTokens);
            }
            if (completionTokens != null) {
                tokenUsage.put("completionTokens", completionTokens);
            }
            if (totalTokens != null) {
                tokenUsage.put("totalTokens", totalTokens);
            }
            if (!tokenUsage.isEmpty()) {
                ext.put("tokenUsage", tokenUsage);
            }

            Object ragHit = findFirstByKeyHints(operParam, "ragHit", "rag_hit", "hit", "rag");
            if (ragHit instanceof Boolean b) {
                ext.put("ragHit", b);
            } else {
                Integer ragDocCount = detectNonEmptyCount(operParam, "retrievedDocs", "retrieved_docs", "documents", "contexts", "chunks");
                if (ragDocCount != null) {
                    ext.put("ragHit", ragDocCount > 0);
                    ext.put("ragDocCount", ragDocCount);
                }
            }

            Object breakdown = findFirstByKeyHints(operParam, "latencyBreakdown", "latency_breakdown", "timing", "timings", "durations", "latencyDetail");
            if (breakdown instanceof Map<?, ?> m) {
                Map<String, Object> latencyBreakdown = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    latencyBreakdown.put(String.valueOf(e.getKey()), e.getValue());
                }
                if (!latencyBreakdown.isEmpty()) {
                    ext.put("latencyBreakdown", latencyBreakdown);
                }
            }
        }
        return ext;
    }

    private Object findFirstByKeyHints(Object root, String... keyHints) {
        if (root == null || keyHints == null || keyHints.length == 0) {
            return null;
        }
        switch (root) {
            case Map<?, ?> map -> {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String k = String.valueOf(e.getKey());
                    Object v = e.getValue();
                    if (isKeyMatch(k, keyHints)) {
                        return v;
                    }
                    Object nested = findFirstByKeyHints(v, keyHints);
                    if (nested != null) {
                        return nested;
                    }
                }
                return null;
            }
            case Collection<?> c -> {
                for (Object it : c) {
                    Object nested = findFirstByKeyHints(it, keyHints);
                    if (nested != null) {
                        return nested;
                    }
                }
                return null;
            }
            case Object[] arr -> {
                for (Object it : arr) {
                    Object nested = findFirstByKeyHints(it, keyHints);
                    if (nested != null) {
                        return nested;
                    }
                }
                return null;
            }
            default -> {
            }
        }
        return null;
    }

    private boolean isKeyMatch(String key, String... keyHints) {
        if (key == null) {
            return false;
        }
        String k = key.toLowerCase().replace("_", "");
        for (String hint : keyHints) {
            if (hint == null || hint.isBlank()) {
                continue;
            }
            String h = hint.toLowerCase().replace("_", "");
            if (k.equals(h) || k.contains(h)) {
                return true;
            }
        }
        return false;
    }

    private Integer detectNonEmptyCount(Object root, String... keyHints) {
        Object v = findFirstByKeyHints(root, keyHints);
        return switch (v) {
            case null -> null;
            case Collection<?> c -> c.size();
            case Object[] arr -> arr.length;
            case Map<?, ?> m -> m.size();
            default -> null;
        };
    }

    private Map<String, Object> buildOperParam(Object[] args) {
        Map<String, Object> out = new LinkedHashMap<>();
        String promptHash = null;

        List<Object> safeArgs = new java.util.ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof ServerWebExchange) {
                    continue;
                }
                Object sanitized = sanitize(arg);
                if (sanitized instanceof Map<?, ?> m) {
                    Object ph = m.get("promptHash");
                    if (ph != null) {
                        promptHash = String.valueOf(ph);
                    }
                }
                safeArgs.add(sanitized);
            }
        }
        out.put("args", safeArgs);
        if (promptHash != null) {
            out.put("promptHash", promptHash);
        }
        return out;
    }

    private Object sanitize(Object arg) {
        if (arg instanceof String s) {
            return sanitizeString(s);
        }
        if (arg instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }
        if (arg instanceof Collection<?> c) {
            return c.stream().limit(20).map(this::sanitize).toList();
        }
        try {
            Map<String, Object> asMap = objectMapper.convertValue(arg, new com.fasterxml.jackson.core.type.TypeReference<>() {
            });
            return sanitizeMap(asMap);
        } catch (Exception e) {
            return truncate(String.valueOf(arg), 500);
        }
    }

    private Object sanitizeString(String s) {
        if (s == null) {
            return null;
        }
        if (s.length() <= 200) {
            return s;
        }
        Map<String, Object> out = new HashMap<>();
        out.put("note", "large_string_omitted");
        out.put("len", s.length());
        out.put("sha256", sha256Hex(s));
        return out;
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        String promptHash = null;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            Object val = e.getValue();
            if (val instanceof String s && isPromptKey(key)) {
                promptHash = sha256Hex(s);
                out.put(key, "omitted");
                continue;
            }
            out.put(key, sanitize(val));
        }
        if (promptHash != null) {
            out.put("promptHash", promptHash);
        }
        return out;
    }

    private boolean isPromptKey(String key) {
        if (key == null) {
            return false;
        }
        String k = key.toLowerCase().replace("_", "");
        return k.contains("prompt") || k.contains("message") || k.contains("input") || k.contains("content");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen);
    }

    private String sha256Hex(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(s.getBytes(StandardCharsets.UTF_8));
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
