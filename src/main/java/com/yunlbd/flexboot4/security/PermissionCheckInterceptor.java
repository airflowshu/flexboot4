package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.config.IgnoreUrlsConfig;
import com.yunlbd.flexboot4.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限校验拦截器
 *
 * 校验逻辑：
 * 1. 有 @RequirePermission 注解 → 使用注解指定的权限码校验
 * 2. 无注解 + BaseController 方法 → 自动生成权限码校验
 * 3. 无注解 + 非 BaseController 方法 → 放行（不做权限控制）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCheckInterceptor implements HandlerInterceptor {

    private final IgnoreUrlsConfig ignoreUrlsConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 0. 检查是否在忽略列表中
        String uri = request.getRequestURI();
        for (String pattern : ignoreUrlsConfig.getUrls()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }

        // 1. 获取当前登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            response.setStatus(401);
            return false;
        }

        // 2. 超级管理员 bypass
        if (loginUser.isSuperAdmin()) {
            return true;
        }

        // 3. 检查方法注解
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation != null) {
            if (annotation.skip()) {
                return true; // 跳过校验
            }
            if (!annotation.value().isEmpty()) {
                // 使用注解指定的权限码
                return checkPermission(loginUser, annotation.value(), request, response);
            }
        }

        // 4. 无注解时，判断是否是 BaseController 方法
        Object controller = handlerMethod.getBean();
        if (controller instanceof BaseController) {
            // BaseController 方法需要自动生成权限码校验
            String requiredPermission = buildPermissionFromRequest(handlerMethod, (BaseController<?, ?, ?>) controller);
            if (requiredPermission != null) {
                return checkPermission(loginUser, requiredPermission, request, response);
            }
        }

        // 5. 非 BaseController 方法且无注解 → 放行（不做权限控制）
        return true;
    }

    /**
     * 权限校验
     */
    private boolean checkPermission(LoginUser loginUser, String requiredPermission,
                                    HttpServletRequest request, HttpServletResponse response) {
        if (!loginUser.hasPermission(requiredPermission)) {
            log.warn("Permission denied: user={}, permission={}, uri={}",
                    loginUser.getSysUser().getUsername(),
                    requiredPermission,
                    request.getRequestURI());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，禁止访问\"}");
            } catch (Exception e) {
                log.error("Failed to write response", e);
            }
            return false;
        }
        return true;
    }

    /**
     * 根据请求自动生成权限码
     * 格式: {entityName}:{operation}
     */
    private String buildPermissionFromRequest(HandlerMethod handlerMethod, BaseController<?, ?, ?> controller) {
        String method = handlerMethod.getMethod().getName();
        String entityName = getEntityName(controller);

        return switch (method) {
            case "create", "saveBatch" -> entityName + ":add";
            case "update" -> entityName + ":edit";
            case "remove", "removeBatch" -> entityName + ":delete";
            case "get", "page", "list" -> entityName + ":list";
            case "exportGet", "exportPost" -> entityName + ":export";
            default -> null;
        };
    }

    /**
     * 获取实体名称（首字母小写 + 冒号分隔）
     * 例如: SysUser -> sys:user
     */
    private String getEntityName(BaseController<?, ?, ?> controller) {
        Class<?> entityClass = controller.getEntityClass();
        if (entityClass == null) {
            return "unknown";
        }
        String simpleName = entityClass.getSimpleName();
        return toSnakeCase(simpleName);
    }

    /**
     * 大写字母转换为冒号分隔的小写形式
     * SysUser -> sys:user
     * SysUserRole -> sys:user:role
     */
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
