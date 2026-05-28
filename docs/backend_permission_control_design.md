# 后端权限控制设计

本文档描述 FlexBoot4 当前 Admin 权限体系。旧版 `BaseController` 已经退出生产代码，当前权限推导围绕 `BaseCrudController` 和显式 `@RequirePermission` 展开。

## 1. 目标

- `/api/admin/**` 默认拒绝未声明权限的接口。
- 通用 CRUD 方法由 `PermissionCheckInterceptor` 自动推导权限码。
- 自定义接口必须显式声明权限码或跳过权限校验。
- 菜单路由和按钮权限码与 vben 5.7.x 的权限设计对齐。
- 超级管理员保留免鉴权能力。

## 2. 请求链路

```text
HTTP Request
  -> JwtAuthenticationFilter
  -> PermissionCheckInterceptor
  -> Controller
```

权限判断顺序：

1. 未登录访问受保护接口，拒绝。
2. 方法或类上存在 `@RequirePermission(skip = true)`，跳过权限码校验。
3. 方法或类上存在 `@RequirePermission("xxx")`，按声明权限码校验。
4. Handler 属于 `BaseCrudController` 通用方法，自动推导 CRUD 权限码。
5. `/api/admin/**` 下仍未得到权限声明，默认拒绝。
6. 非 Admin 管理接口不走 Admin 权限默认拒绝策略。

## 3. 权限码规范

```text
{domain}:{resource}:{operation}
```

标准 CRUD 操作：

| operation | 方法 | 路径 |
| --- | --- | --- |
| `list` | `GET` / `POST` | `/{id}`、`/page`、`/list` |
| `add` | `POST` | `/` |
| `edit` | `PUT` | `/{id}` |
| `delete` | `DELETE` | `/{id}`、`/` |
| `export` | `GET` / `POST` | `/export` |
| `import` | `POST` | `/import` |

示例：

| 资源 | 列表 | 新增 | 修改 | 删除 | 导出 | 导入 |
| --- | --- | --- | --- | --- | --- | --- |
| 用户 | `sys:user:list` | `sys:user:add` | `sys:user:edit` | `sys:user:delete` | `sys:user:export` | `sys:user:import` |
| 角色 | `sys:role:list` | `sys:role:add` | `sys:role:edit` | `sys:role:delete` | `sys:role:export` | `sys:role:import` |
| 菜单 | `sys:menu:list` | `sys:menu:add` | `sys:menu:edit` | `sys:menu:delete` | `sys:menu:export` | `sys:menu:import` |

## 4. CRUD 权限推导

`PermissionCheckInterceptor` 只识别 `BaseCrudController`。生产代码不再兼容旧 `BaseController`。

示例 Controller：

```java
@RestController
@RequestMapping("/api/admin/user")
public class SysUserController extends BaseCrudController<SysUserService, SysUser, String,
        SysUserCreateReq, SysUserUpdateReq, SysUserListVO, SysUserDetailVO> {
    // ...
}
```

权限前缀来自资源路径或框架约定。例如 `/api/admin/user/page` 推导为：

```text
sys:user:list
```

对于方法名，`create` 映射为 `add`，`update` 映射为 `edit`，`remove` 和 `removeBatch` 映射为 `delete`。

## 5. 自定义接口

自定义接口必须显式声明权限：

```java
@RequirePermission("sys:user:reset-password")
@PostMapping("/{id}/reset-password")
public ApiResult<Boolean> resetPassword(@PathVariable String id) {
    // ...
}
```

确实无需权限码的接口应显式声明跳过：

```java
@RequirePermission(skip = true)
@GetMapping("/all")
public ApiResult<List<VueRoute>> getAllMenus() {
    // ...
}
```

不要依赖“无注解自动放行”。Admin 管理接口默认拒绝无声明接口。

## 6. vben 菜单与按钮权限

vben 对接规则：

- 后端路由接口返回 catalog/menu 路由树。
- button 节点不进入路由树。
- button 节点的 `authCode` 进入 `/admin/auth/codes`。
- `RouteMeta` 使用 vben 字段：`hideInMenu`、`hideInTab`、`hideInBreadcrumb`。
- 布局组件与前端 `layoutMap` 保持一致。

菜单和按钮示例：

```text
系统管理
  用户管理                  -> route
    新增用户 sys:user:add    -> button
    编辑用户 sys:user:edit   -> button
    删除用户 sys:user:delete -> button
```

## 7. LoginUser 与权限加载

登录成功后，认证用户上下文应包含：

- 用户基础信息
- 角色标识
- 权限码列表

权限码来自用户角色绑定的菜单/按钮节点。超级管理员用户拥有所有权限。

## 8. 缓存与权限变更

当角色、菜单或用户角色关系变化时，应清理或刷新用户权限缓存，避免登录态中的权限码滞后。

典型触发点：

- 分配用户角色
- 分配角色菜单
- 禁用菜单或按钮权限
- 修改菜单 `authCode`

## 9. 新接口接入清单

1. Admin 管理接口路径放在 `/api/admin/**`。
2. 通用 CRUD 继承 `BaseCrudController` 或 `EntityCrudController`。
3. 自定义接口声明 `@RequirePermission`。
4. 在 `sys_menu` 中添加对应 button 权限节点。
5. 给目标角色分配菜单/按钮。
6. 前端按钮使用同一权限码控制显示。
7. 通过接口请求验证 401、403、成功路径。

## 10. 常见错误

### 前端按钮显示，但接口 403

通常是前端按钮权限码和后端推导权限码不一致。例如前端使用 `sys:user:create`，但后端标准是 `sys:user:add`。

### 登录后菜单不显示

检查：

- 菜单节点是否是 catalog/menu，而不是 button。
- 根节点 `parentId` 是否为 `NULL`。
- `component` 是否为 vben 支持的 `BasicLayout` 或真实页面路径。
- `status` 是否启用。

### 路由接口返回 button 节点

这是错误设计。button 节点应只进入权限码列表，不进入动态路由树。
