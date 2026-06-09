package com.yunlbd.flexboot4.aigateway.security;

import com.yunlbd.flexboot4.auth.jwt.JwtClaimKeys;
import com.yunlbd.flexboot4.auth.jwt.JwtScopes;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@Aspect
@Component
public class AiRequirePermissionAspect {

    @Around("@annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Object result = joinPoint.proceed();
        if (requirePermission.skip() || requirePermission.value().isBlank()) {
            return result;
        }

        if (result instanceof Mono<?> mono) {
            return Mono.deferContextual(ctx -> {
                Claims claims = ctx.getOrDefault(Claims.class, null);
                ServerWebExchange exchange = ctx.getOrDefault(ServerWebExchange.class, null);
                ensurePermission(claims, exchange, requirePermission.value());
                return mono;
            });
        }

        if (result instanceof Flux<?> flux) {
            return Flux.deferContextual(ctx -> {
                Claims claims = ctx.getOrDefault(Claims.class, null);
                ServerWebExchange exchange = ctx.getOrDefault(ServerWebExchange.class, null);
                ensurePermission(claims, exchange, requirePermission.value());
                return flux;
            });
        }

        return result;
    }

    private static void ensurePermission(Claims claims, ServerWebExchange exchange, String requiredPermission) {
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        ApiKeyRule rule = exchange == null
                ? null
                : exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_API_KEY_RULE);
        if (rule != null && hasConfiguredPermission(rule.permissions(), requiredPermission)) {
            return;
        }
        if (hasPermission(claims.get(JwtClaimKeys.PERMISSIONS), requiredPermission)) {
            return;
        }
        if ((rule == null || rule.permissions() == null || rule.permissions().isEmpty())
                && isAiPermission(requiredPermission)
                && hasAiScope(claims.get(JwtClaimKeys.SCOPE))) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private static boolean hasConfiguredPermission(Collection<String> permissions, String requiredPermission) {
        return permissions != null
                && !permissions.isEmpty()
                && hasPermission(permissions, requiredPermission);
    }

    private static boolean hasPermission(Object permissions, String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isBlank()) {
            return true;
        }
        if (permissions == null) {
            return false;
        }
        if (permissions instanceof String s) {
            return requiredPermission.equals(s);
        }
        if (permissions instanceof Collection<?> c) {
            for (Object it : c) {
                if (requiredPermission.equals(String.valueOf(it))) {
                    return true;
                }
            }
            return false;
        }
        if (permissions instanceof Object[] arr) {
            return hasPermission(List.of(arr), requiredPermission);
        }
        return false;
    }

    private static boolean isAiPermission(String requiredPermission) {
        return requiredPermission != null && requiredPermission.startsWith("ai:");
    }

    private static boolean hasAiScope(Object scope) {
        if (scope == null) {
            return false;
        }
        if (scope instanceof String s) {
            return JwtScopes.AI.equals(s);
        }
        if (scope instanceof Collection<?> c) {
            for (Object it : c) {
                if (JwtScopes.AI.equals(String.valueOf(it))) {
                    return true;
                }
            }
            return false;
        }
        if (scope instanceof Object[] arr) {
            return hasAiScope(List.of(arr));
        }
        return false;
    }
}

