# 后端接口权限控制实现方案

## 1. 需求背景

### 当前架构
- **BaseController**：通用 CRUD 控制器，所有业务 Controller 继承它
  - 提供：`create`, `update`, `saveBatch`, `remove`, `removeBatch`, `get`, `page`, `list`, `export`
- **业务 Controller**：继承 BaseController，可添加自定义业务接口
- **超级管理员**：`SYS_SUPER_USER_ID = "1"`，拥有所有权限，免鉴权

### 目标
- **BaseController 的 CRUD 方法**：自动根据实体类型生成权限码校验，无需额外配置
- **业务 Controller 的自定义接口**：按需添加 `@RequirePermission` 注解进行校验
- **无需权限控制的接口**：不加注解即可

---

## 2. 权限码设计

### 2.1 权限码命名规范

```
{module}:{entity}:{operation}
```

| operation | HTTP Method                       | 说明           |
|-----------|-----------------------------------|--------------|
| `list` | POST /page, POST /list, GET /{id} | 列表/分页查询、单条查询 |
| `add` | POST, POST /batch                 | 新增 、批量新增     |        
| `edit` | PUT /{id}                         | 修改           |
| `delete` | DELETE /{id}, DELETE /batch       | 删除、批量删除      |
| `export` | GET/POST /export                  | 导出           |

### 2.2 权限码示例

| 实体类型 | 列表 | 新增 | 修改 | 删除 | 导出 |
|---------|------|------|------|------|------|
| SysUser | `sys:user:list` | `sys:user:add` | `sys:user:edit` | `sys:user:delete` | `sys:user:export` |
| SysRole | `sys:role:list` | `sys:role:add` | `sys:role:edit` | `sys:role:delete` | `sys:role:export` |
| SysDept | `sys:dept:list` | `sys:dept:add` | `sys:dept:edit` | `sys:dept:delete` | `sys:dept:export` |

---

## 3. 实现方案

### 3.1 整体架构

```
请求 → JwtAuthenticationFilter(认证) → PermissionCheckInterceptor(权限校验) → Controller
                                                                          │
                              ┌────────────────────┬────────────────────┘
                              │                    │
                    BaseController 方法          自定义方法
                     (自动生成权限码)          (@RequirePermission 注解)
```

### 3.2 校验逻辑

1. **无注解** + BaseController 方法 → 自动生成权限码校验
2. **有注解** + 任意方法 → 使用注解指定的权限码校验
3. **无注解** + 非 BaseController 方法 → 放行（不做权限控制）

---

## 4. 实现步骤

### 4.1 扩展 LoginUser 存储权限码

**文件**: `src/main/java/com/yunlbd/flexboot4/security/LoginUser.java`

```java
public class LoginUser implements UserDetails {

    private SysUser sysUser;
    private Collection<? extends GrantedAuthority> authorities;

    /** 用户拥有的权限码列表 */
    private List<String> permissionCodes;

    // ... 构造方法和现有字段 ...

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }

    /** 检查是否拥有指定权限码 */
    public boolean hasPermission(String code) {
        return permissionCodes != null && permissionCodes.contains(code);
    }

    /** 是否为超级管理员 */
    public boolean isSuperAdmin() {
        return SysConstant.SYS_SUPER_USER_ID.equals(sysUser.getId());
    }
}
```

### 4.2 修改 UserDetailsServiceImpl 加载权限码

**文件**: `src/main/java/com/yunlbd/flexboot4/security/UserDetailsServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "userDetails")
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;
    private final SysMenuService sysMenuService;

    @Override
    @Cacheable(key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) {
        SysUser sysUser = sysUserService.getOne(
            QueryWrapper.create().eq(SysUser::getUsername, username)
        );

        if (sysUser == null) {
            return null;
        }

        // 构建角色权限 (ROLE_xxx 格式)
        List<SimpleGrantedAuthority> authorities = sysUser.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleValue()))
            .collect(Collectors.toList());

        LoginUser loginUser = new LoginUser(sysUser, authorities);

        // 加载用户的权限码列表
        List<String> permissionCodes = sysMenuService.getPermissionCodes(sysUser.getId());
        loginUser.setPermissionCodes(permissionCodes);

        return loginUser;
    }
}
```

### 4.3 创建权限注解

**文件**: `src/main/java/com/yunlbd/flexboot4/common/annotation/RequirePermission.java`

```java
package com.yunlbd.flexboot4.common.annotation;

import java.lang.annotation.*;

/**
 * 接口权限注解
 * 标注在 Controller 方法上，指定访问该接口所需的权限码
 *
 * @usage
 * // 自定义权限码
 * @RequirePermission("sys:user:resetPwd")
 * @PostMapping("/reset-password")
 * public ApiResult<?> resetPassword() { ... }
 *
 * // 跳过权限校验（特殊情况）
 * @RequirePermission(skip = true)
 * @GetMapping("/public")
 * public ApiResult<?> publicApi() { ... }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 所需权限码 */
    String value() default "";

    /** 跳过权限校验（用于特殊情况，如公开接口） */
    boolean skip() default false;
}
```

### 4.4 创建权限校验拦截器

**文件**: `src/main/java/com/yunlbd/flexboot4/security/PermissionCheckInterceptor.java`

```java
package com.yunlbd.flexboot4.security;

import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.constant.SysConstant;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.controller.BaseController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
public class PermissionCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
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
                return checkPermission(loginUser, annotation.value(), request);
            }
        }

        // 4. 无注解时，判断是否是 BaseController 方法
        Object controller = handlerMethod.getBean();
        if (controller instanceof BaseController) {
            // BaseController 方法需要自动生成权限码校验
            String requiredPermission = buildPermissionFromRequest(handlerMethod, (BaseController<?, ?, ?>) controller);
            if (requiredPermission != null) {
                return checkPermission(loginUser, requiredPermission, request);
            }
        }

        // 5. 非 BaseController 方法且无注解 → 放行（不做权限控制）
        return true;
    }

    /**
     * 权限校验
     */
    private boolean checkPermission(LoginUser loginUser, String requiredPermission, HttpServletRequest request) {
        if (!loginUser.hasPermission(requiredPermission)) {
            log.warn("Permission denied: user={}, permission={}, uri={}",
                    loginUser.getSysUser().getUsername(),
                    requiredPermission,
                    request.getRequestURI());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，禁止访问\"}");
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
```

### 4.5 注册拦截器

**文件**: `src/main/java/com/yunlbd/flexboot4/security/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PermissionCheckInterceptor permissionCheckInterceptor;
    // ... 其他依赖

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(ignoreUrlsConfig.getUrls().toArray(new String[0])).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(permissionCheckInterceptor, JwtAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 5. 使用示例

### 5.1 业务 Controller 完整示例

```java
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SysUserController extends BaseController<ISysUserService, SysUser, String> {

    private final ISysUserService userService;  // 额外注入业务服务

    @Override
    protected Class<SysUser> getEntityClass() {
        return SysUser.class;
    }

    // ==================== CRUD 方法（自动受权限控制） ====================
    // POST /       → sys:user:add
    // PUT /{id}    → sys:user:edit
    // DELETE /{id} → sys:user:delete
    // GET /{id}    → sys:user:list
    // POST /page   → sys:user:list
    // POST /export → sys:user:export

    // ==================== 自定义业务接口 ====================

    // 按需添加权限注解
    @RequirePermission("sys:user:resetPwd")
    @PostMapping("/reset-password")
    public ApiResult<Void> resetPassword(@RequestBody ResetPwdReq req) {
        userService.resetPassword(req);
        return ApiResult.success();
    }

    // 无需权限控制的接口（不加注解）
    @GetMapping("/options")
    public ApiResult<List<Option>> getOptions() {
        return ApiResult.success(userService.getOptions());
    }

    // 特殊情况：跳过权限校验
    @RequirePermission(skip = true)
    @GetMapping("/public-info")
    public ApiResult<?> getPublicInfo() {
        return ApiResult.success(...);
    }
}
```

### 5.2 自定义权限码覆盖

```java
public class SysUserController extends BaseController<...> {

    // 覆盖默认权限码，使用自定义权限
    @RequirePermission("sys:user:manage")
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> remove(@PathVariable String id) {
        return super.remove(id);
    }
}
```

---

## 6. 数据权限扩展（可选）

后续如需实现**数据权限**（用户只能查看/操作自己部门的数据），可在 Service 层扩展：

```java
public interface IExtendedService<T> {
    // 现有方法 ...

    /**
     * 获取数据权限过滤条件
     */
    default QueryWrapper getDataScopeFilter(String userId) {
        return QueryWrapper.create();
    }
}
```

在 `page()` 和 `list()` 方法中自动追加数据权限过滤条件。

---

## 7. 实施计划

| 步骤 | 任务 | 文件 |
|------|------|------|
| 1 | 扩展 `LoginUser` 添加权限码字段和 `isSuperAdmin()` | `security/LoginUser.java` |
| 2 | 修改 `UserDetailsServiceImpl` 加载权限码 | `security/UserDetailsServiceImpl.java` |
| 3 | 创建 `@RequirePermission` 注解 | `common/annotation/RequirePermission.java` |
| 4 | 创建 `PermissionCheckInterceptor` | `security/PermissionCheckInterceptor.java` |
| 5 | 注册拦截器到 `SecurityConfig` | `security/SecurityConfig.java` |

---

## 8. 缓存与权限变更

当管理员修改用户角色或菜单权限时，需清理 `userDetails` 缓存：

```java
// 在角色/权限变更时调用
userDetailsService.evictUserCache(username);
```

缓存配置已在 `UserDetailsServiceImpl` 上通过 `@CacheConfig` 注解实现。

---

## 9. 权限码对照表

### BaseController 自动生成

| 方法名 | HTTP 路径 | 自动生成权限码 |
|--------|-----------|----------------|
| `create` | POST / | `{entity}:add` |
| `saveBatch` | POST /batch | `{entity}:add` |
| `update` | PUT /{id} | `{entity}:edit` |
| `remove` | DELETE /{id} | `{entity}:delete` |
| `removeBatch` | DELETE / | `{entity}:delete` |
| `get` | GET /{id} | `{entity}:list` |
| `page` | POST /page | `{entity}:list` |
| `list` | POST /list | `{entity}:list` |
| `exportGet/exportPost` | GET/POST /export | `{entity}:export` |

### 实体类型映射

| 实体类 | 权限码前缀 |
|--------|-----------|
| SysUser | `sys:user` |
| SysRole | `sys:role` |
| SysDept | `sys:dept` |
| SysMenu | `sys:menu` |
| SysUserRole | `sys:user:role` |
