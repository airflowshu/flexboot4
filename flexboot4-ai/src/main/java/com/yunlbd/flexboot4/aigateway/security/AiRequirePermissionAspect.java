package com.yunlbd.flexboot4.aigateway.security;

import com.yunlbd.flexboot4.auth.jwt.JwtClaimKeys;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import io.jsonwebtoken.Claims;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
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
                ensurePermission(claims, requirePermission.value());
                return mono;
            });
        }

        if (result instanceof Flux<?> flux) {
            return Flux.deferContextual(ctx -> {
                Claims claims = ctx.getOrDefault(Claims.class, null);
                ensurePermission(claims, requirePermission.value());
                return flux;
            });
        }

        return result;
    }

    private static void ensurePermission(Claims claims, String requiredPermission) {
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Object permissions = claims.get(JwtClaimKeys.PERMISSIONS);
        if (!hasPermission(permissions, requiredPermission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
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
}

