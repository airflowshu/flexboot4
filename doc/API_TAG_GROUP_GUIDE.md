# OpenAPI 标签分组使用指南

## 概述

FlexBoot4 Admin 现已支持使用 `@ApiTagGroup` 注解直接在 Controller 类上声明 API 标签分组，无需在 `OpenApiConfig` 中进行手动维护。该方案具有以下优势：

- **自动扫描**：应用启动时自动扫描所有标记的 Controller
- **单一维护点**：分组信息在 Controller 上声明，无需重复配置
- **易于扩展**：添加新的 Controller 时，只需加上注解即可
- **向后兼容**：保留默认配置作为备选方案

## 架构

### 核心组件

1. **`ApiTagGroup` 注解**
   - 位置：`com.yunlbd.flexboot4.config.ApiTagGroup`
   - 作用：在 Controller 类上标记 API 标签分组

2. **`ApiTagGroupInfo` 数据模型**
   - 位置：`com.yunlbd.flexboot4.config.ApiTagGroupInfo`
   - 作用：存储和管理分组的元数据（分组名称、标签集合）

3. **`OpenApiTagGroupScanner` 扫描器**
   - 位置：`com.yunlbd.flexboot4.config.OpenApiTagGroupScanner`
   - 作用：在应用启动时自动扫描所有 `@ApiTagGroup` 标记的 Controller

4. **`OpenApiConfig` 配置类**
   - 位置：`com.yunlbd.flexboot4.config.OpenApiConfig`
   - 作用：构建 OpenAPI 配置，优先使用扫描器结果，备选使用默认配置

## 使用示例

### 基本用法

在 Controller 类上同时添加 `@Tag` 和 `@ApiTagGroup` 注解：

```java
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "SysUser - 用户管理")
@ApiTagGroup(group = "系统管理")
public class SysUserController extends BaseController<SysUserService, SysUser, String> {
    // ...
}
```

### 参数说明

#### `@Tag` 注解
- `name`：标签名称（**必填**），必须与分组中的标签相匹配
- `description`：标签描述（可选）

#### `@ApiTagGroup` 注解
- `group`：分组名称（**必填**），例如："系统管理"、"运维管理"
- `enabled`：是否启用此分组配置（可选，默认为 `true`）

### 完整示例

```java
@RestController
@RequestMapping("/api/admin/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "SysRole - 角色管理")
@ApiTagGroup(group = "系统管理")
public class SysRoleController extends BaseController<SysRoleService, SysRole, String> {

    @Override
    public Class<SysRole> getEntityClass() {
        return SysRole.class;
    }
}
```

## 已应用的分组映射

### 系统管理（`system`）
| 标签名 | 对应 Controller |
|------|----------------|
| 认证管理 | `AuthController` |
| 用户管理 | `SysUserController` |
| 部门管理 | `SysDeptController` |
| 角色管理 | `SysRoleController` |
| 菜单管理 | `SysMenuController` |
| 字典管理 | `SysDictTypeController`、`SysDictItemController` |
| 权限管理 | `SysRoleMenuController`、`SysUserRoleController` |
| 版本日志 | `SysVersionLogController` |

### 运维管理（`ops`）
| 标签名 | 对应 Controller |
|------|----------------|
| 操作日志 | `SysOperLogController` |
| 文件管理 | `SysFileController` |
| 系统配置 | `SysConfigController` |
| apiKey管理 | `AiApiKeyController` |
| 系统监控 | `SysMonitorController` |

### 知识库（`kb`）
| 标签名 | 对应 Controller |
|------|----------------|
| 知识库管理 | 在其他模块中 |

## 工作流程

1. **应用启动时**
   - Spring 容器初始化
   - `OpenApiTagGroupScanner` 实现 `BeanFactoryPostProcessor` 接口，在 Bean 工厂后处理阶段执行
   - 扫描所有 `@RestController` 和 `@Controller` 标记的类
   - 收集所有被 `@ApiTagGroup` 注解标记的类
   - 从 `@Tag` 注解中提取标签名称，添加到对应的分组中

2. **配置 OpenAPI**
   - `OpenApiConfig` 的 `openAPI()` Bean 被创建
   - 调用 `OpenApiTagGroupScanner.getTagGroupsForOpenAPI()` 获取扫描结果
   - 若扫描结果不为空，使用扫描结果配置 `x-tagGroups`
   - 若扫描结果为空，使用 `getDefaultTagGroups()` 中的备选配置

3. **API 文档生成**
   - Swagger UI / Scalar UI 根据 `x-tagGroups` 配置渲染分组

## 日志输出

应用启动时，可在日志中看到：

```
INFO OpenApiTagGroupScanner - OpenAPI tag groups scanned. Total groups: 2, Total tags: 13
DEBUG OpenApiTagGroupScanner - Registered API tag group: Controller=SysUserController, Group=系统管理, Tag=用户管理
DEBUG OpenApiTagGroupScanner - Registered API tag group: Controller=SysRoleController, Group=系统管理, Tag=角色管理
...
INFO OpenApiConfig - OpenAPI tag groups loaded from scanner: 2 groups
```

## 扩展新的 Controller

当添加新的 Controller 时，只需执行以下步骤：

1. 确保 Controller 被 `@RestController` 或 `@Controller` 标记
2. 添加 `@Tag(name = "你的标签名")` 注解
3. 添加 `@ApiTagGroup(group = "你的分组名")` 注解
4. 应用启动时，新的分组会自动被扫描和注册

**不需要**修改 `OpenApiConfig`！

## 禁用分组（可选）

如果某个 Controller 的分组配置需要暂时禁用，可以设置 `enabled = false`：

```java
@ApiTagGroup(group = "系统管理", enabled = false)
public class MyController {
    // ...
}
```

此时该 Controller 的标签不会被扫描器收集。

## 备选方案

如果扫描器未扫描到任何分组（例如所有 Controller 都禁用了），则会使用 `OpenApiConfig.getDefaultTagGroups()` 中定义的默认配置。

默认配置保留了与原 `OpenApiConfig` 相同的分组结构，确保向后兼容。

## 常见问题

### Q: 如果 Controller 没有 @Tag 注解会怎样？
A: 该 Controller 不会被扫描器收集，其标签不会被添加到任何分组中。建议始终同时使用 `@Tag` 和 `@ApiTagGroup` 注解。

### Q: 能否将一个 Controller 添加到多个分组？
A: 当前版本支持一个 Controller 属于一个分组。如需支持多分组，可考虑扩展注解为 `@ApiTagGroup(groups = {...})`。

### Q: 修改注解后需要重新启动应用吗？
A: 是的，需要重新启动应用。扫描是在应用启动时执行的。

### Q: 为什么日志中没有显示我的 Controller？
A: 请检查：
1. Controller 是否被 `@RestController` 或 `@Controller` 标记
2. Controller 是否被 `@ApiTagGroup` 标记
3. Controller 是否被 `@Tag` 标记（且 `name` 不为空）
4. 应用日志级别是否设为 `DEBUG`（以查看详细日志）

## 相关文件

- 注解定义：`src/main/java/com/yunlbd/flexboot4/config/ApiTagGroup.java`
- 数据模型：`src/main/java/com/yunlbd/flexboot4/config/ApiTagGroupInfo.java`
- 扫描器实现：`src/main/java/com/yunlbd/flexboot4/config/OpenApiTagGroupScanner.java`
- OpenAPI 配置：`src/main/java/com/yunlbd/flexboot4/config/OpenApiConfig.java`

---

**最后更新**：2026-02-26  
**作者**：Flexboot4 Team

