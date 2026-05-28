package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import com.yunlbd.flexboot4.controller.sys.BaseCrudController;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Permission interceptor.
 *
 * Rule:
 * 1. If method has @RequirePermission, obey it.
 * 2. If method is inherited from BaseCrudController, auto-resolve permission code.
 * 3. For /api/admin/**, methods without annotation are denied by default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCheckInterceptor implements HandlerInterceptor {

    private final IgnoreUrlsConfig ignoreUrlsConfig;
    private final MetricsRecorder metricsRecorder;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();
        for (String pattern : ignoreUrlsConfig.getUrls()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            metricsRecorder.increment("flexboot4.auth.anonymous_denied", Map.of("uri", uri));
            response.setStatus(401);
            return false;
        }

        if (loginUser.isSuperAdmin()) {
            return true;
        }

        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation != null) {
            if (annotation.skip()) {
                return true;
            }
            if (!annotation.value().isEmpty()) {
                return checkPermission(loginUser, annotation.value(), request, response);
            }
        }

        Object controller = handlerMethod.getBean();
        if (controller instanceof BaseCrudController<?, ?, ?, ?, ?, ?, ?> baseCrudController) {
            String requiredPermission = buildPermissionFromRequest(handlerMethod, baseCrudController);
            if (requiredPermission != null) {
                return checkPermission(loginUser, requiredPermission, request, response);
            }
        }
        if (uri.startsWith("/api/admin/")) {
            metricsRecorder.increment("flexboot4.permission.denied", Map.of(
                    "uri", uri,
                    "reason", "missing_annotation"
            ));
            return writeForbidden(response, "Missing permission annotation");
        }

        return true;
    }

    private boolean checkPermission(LoginUser loginUser,
                                    String requiredPermission,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        if (!loginUser.hasPermission(requiredPermission)) {
            log.warn("Permission denied: user={}, permission={}, uri={}",
                    loginUser.getSysUser().getUsername(),
                    requiredPermission,
                    request.getRequestURI());
            metricsRecorder.increment("flexboot4.permission.denied", Map.of(
                    "uri", request.getRequestURI(),
                    "permission", requiredPermission,
                    "reason", "missing_permission"
            ));
            return writeForbidden(response, "Permission denied");
        }
        return true;
    }

    private boolean writeForbidden(HttpServletResponse response, String reason) {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，禁止访问\"}");
        } catch (Exception e) {
            log.error("Failed to write forbidden response, reason={}", reason, e);
        }
        return false;
    }

    /**
     * Build permission from BaseCrudController method name.
     * Format: {entity}:{operation}
     */
    private String buildPermissionFromRequest(HandlerMethod handlerMethod, BaseCrudController<?, ?, ?, ?, ?, ?, ?> controller) {
        String method = handlerMethod.getMethod().getName();
        String entityName = getEntityName(controller);

        return switch (method) {
            case "create" -> entityName + ":add";
            case "update" -> entityName + ":edit";
            case "remove", "removeBatch" -> entityName + ":delete";
            case "get", "page", "list" -> entityName + ":list";
            case "exportGet", "exportPost" -> entityName + ":export";
            case "importExcel" -> entityName + ":import";
            default -> null;
        };
    }

    private String getEntityName(BaseCrudController<?, ?, ?, ?, ?, ?, ?> controller) {
        Class<?> entityClass = controller.getEntityClass();
        if (entityClass == null) {
            return "unknown";
        }
        return toSnakeCase(entityClass.getSimpleName());
    }

    private String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append(':');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
