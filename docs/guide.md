# FlexBoot4 开发者指南

本文档是 FlexBoot4 当前后端框架范式的主入口，面向使用和扩展本工程的开发者。旧的 Entity 直通式 CRUD 已经不再作为推荐方式，当前标准是 DTO/VO 安全边界、MapStruct 显式转换、权限码自动推导和可低代码生成。

## 1. 模块分层

```text
flexboot4-core
  基础 DTO、枚举、工具，不依赖 Spring。

flexboot4-admin-kernel
  Starter 公共底座，提供 BaseCrudController、EntityCrudController、
  CrudFieldPolicy、CrudExcelSupport、DefaultCrudMapper、查询构建器等。

flexboot4-admin-starter
  Admin/RBAC/系统管理/运维能力，包含用户、角色、菜单、配置、文件、API Key 等。

业务 starter
  CMS、Media、SMS4J、KB 等按需引入，原则上只依赖 admin-kernel。

flexboot4-bootstrap
  内部开发聚合应用，用于本仓库联调。

flexboot4-ai
  AI Gateway 独立服务。
```

更多模块文档见 [模块文档索引](modules.md)。AI 厂商模型、DeepSeek V4 与 APISIX 接入见 [AI 厂商模型接入指南](ai-provider-models.md)。

## 2. CRUD 范式

### 标准类型

每个可管理资源应显式区分请求、响应、Excel 与实体模型：

```text
dto/{domain}/{Entity}CreateReq
dto/{domain}/{Entity}UpdateReq
vo/{domain}/{Entity}ListVO
vo/{domain}/{Entity}DetailVO
excel/{domain}/{Entity}ExportRow
excel/{domain}/{Entity}ImportRow   # 仅开启导入时提供
converter/{domain}/{Entity}CrudMapper
```

Req 不允许包含：

- `id`
- `version`
- `delFlag`
- `createTime`
- `lastModifyTime`
- `createBy`
- `lastModifyBy`
- Entity 关系对象

ListVO 和 DetailVO 是 API 输出契约，不直接复用 Entity。DetailVO 可以包含关系数据，但必须是安全 VO。

### 高风险模块

以下模块必须使用显式 DTO/VO/ExcelRow/MapStruct，不允许使用默认身份映射：

- `SysUser`
- `SysRole`
- `SysMenu`
- `SysConfig`
- `AiApiKey`
- `SysFile`

示例：

```java
@RestController
@RequestMapping("/api/admin/user")
public class SysUserController extends BaseCrudController<SysUserService, SysUser, String,
        SysUserCreateReq, SysUserUpdateReq, SysUserListVO, SysUserDetailVO> {

    public SysUserController(SysUserService service, SysUserCrudMapper mapper) {
        super(service, mapper);
    }

    @Override
    public Class<SysUser> getEntityClass() {
        return SysUser.class;
    }
}
```

### 普通基础表

低风险、基础配置类表可以使用 `EntityCrudController` 快速接入，但泛型仍必须是 DTO/VO：

```java
public class SysDictTypeController extends EntityCrudController<SysDictTypeService, SysDictType, String,
        SysDictTypeCreateReq, SysDictTypeUpdateReq, SysDictTypeListVO, SysDictTypeDetailVO> {

    public SysDictTypeController(SysDictTypeService service) {
        super(service, SysDictType.class, SysDictTypeListVO.class, SysDictTypeDetailVO.class);
    }
}
```

`EntityCrudController` 内部使用 `DefaultCrudMapper`，只适合低风险模块。它会忽略 Entity 上多出来但 VO 未声明的字段，避免关系对象污染列表响应。

### 不适合通用 CRUD 的场景

纯关系表或流程型接口不要暴露通用 CRUD，应收口为明确业务接口：

- 用户分配角色
- 角色分配菜单
- 文章绑定/解绑标签
- 审核、发布、重置密码等流程动作

## 3. CRUD 接口

`BaseCrudController` 固定提供：

| 方法 | 路径 | 说明 | 权限动作 |
| --- | --- | --- | --- |
| `POST` | `/` | 新增 | `add` |
| `PUT` | `/{id}` | 修改 | `edit` |
| `DELETE` | `/{id}` | 删除 | `delete` |
| `DELETE` | `/` | 批量删除 | `delete` |
| `GET` | `/{id}` | 详情 | `list` |
| `POST` | `/page` | 分页查询 | `list` |
| `POST` | `/list` | 列表查询 | `list` |
| `GET/POST` | `/export` | 导出 | `export` |
| `POST` | `/import` | 导入，默认关闭 | `import` |

## 4. 查询与排序

查询入参统一使用 `SearchDto`：

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "logic": "AND",
  "items": [
    { "field": "sysUser.status", "op": "eq", "val": 1 },
    {
      "logic": "OR",
      "children": [
        { "field": "sysUser.username", "op": "like", "val": "admin" },
        { "field": "sysUser.realName", "op": "like", "val": "管理员" }
      ]
    }
  ],
  "orders": [
    { "column": "sysUser.createTime", "asc": false }
  ]
}
```

字段命名规则：

- 根表字段可以使用裸字段，如 `createTime`。
- 根表字段也可以使用根实体别名或表名驼峰前缀，如 `sysUser.createTime`、`sysDictType.code`。
- 关系字段使用关系属性名、目标实体名驼峰或目标表名驼峰，如 `dept.deptName`、`sysDept.deptName`。
- 查询和排序字段必须通过 `CrudFieldPolicy` 白名单。
- 关系字段必须由 Controller 显式开放，不能因为同名根字段存在就自动放行。

高风险模块应显式声明字段白名单：

```java
@Override
protected CrudFieldPolicy fieldPolicy() {
    return CrudFieldPolicy.same(List.of(
            "id", "username", "realName", "email", "phone",
            "deptId", "status", "remark", "createTime", "lastModifyTime"
    )).withQueryFields("dept.deptName", "roles.roleValue", "roles.roleName");
}
```

更多查询细节见 [通用查询构建与使用说明](Mf基础功能.md)。

## 5. 权限设计

权限码统一使用：

```text
{domain}:{resource}:list
{domain}:{resource}:add
{domain}:{resource}:edit
{domain}:{resource}:delete
{domain}:{resource}:export
{domain}:{resource}:import
```

示例：

```text
sys:user:list
sys:user:add
sys:user:edit
sys:user:delete
sys:user:export
sys:user:import
```

规则：

- `/api/admin/**` 默认拒绝无权限声明的接口。
- 继承 `BaseCrudController` 的通用方法由 `PermissionCheckInterceptor` 自动推导权限码。
- 自定义接口必须显式使用 `@RequirePermission("xxx")`。
- 登录、OpenAPI、静态资源、路由接口等少量接口可使用 `@RequirePermission(skip = true)` 或白名单。
- 超级管理员用户仍可跳过权限码校验。

更多细节见 [后端权限控制设计](backend_permission_control_design.md)。

## 6. vben 路由与菜单

后端菜单与 vben 5.7.x 的契约保持一致：

- 后端路由接口只返回 catalog/menu 路由树。
- button 节点不进入路由树，只作为按钮权限码进入 `/admin/auth/codes`。
- `RouteMeta` 字段使用 vben 标准命名：`hideInMenu`、`hideInTab`、`hideInBreadcrumb`。
- 布局组件使用前端 `layoutMap` 支持的值，例如 `BasicLayout`、`IFrameView`。
- 根菜单 parentId 统一使用 `NULL`，不要混用 `"0"`。

模块的 Flyway 菜单迁移脚本应包含：

- 菜单路由节点
- 按钮权限节点
- `authCode`
- vben meta 字段

`sys_role_menu` 授权数据按模块或项目初始化策略追加；可插拔业务 starter 不强制默认授权给内置角色。

已有开发数据不需要兼容旧格式，旧数据不符合规范时直接用 SQL 修正。

## 7. 文件存储

文件管理统一通过 `FileStorage` 抽象接入，默认使用本地存储。新上传文件的存储类型由 `flexboot4.file-storage.type` 决定，历史文件按 `sys_file.storage_type` 路由读取。因此 `type: local` 只表示新上传走本地；如果库里仍有 `storage_type=MINIO` 的历史文件，仍需保留可用的 `minio.*` 连接参数，让 MinIO 存储实现被装配用于读取历史文件。

```yaml
flexboot4:
  file-storage:
    type: local # local | minio
    local:
      root-dir: ${user.home}/flexboot4-files
      bucket: local
```

切换到 MinIO 时设置：

```yaml
flexboot4:
  file-storage:
    type: minio

minio:
  endpoint: http://127.0.0.1:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: flexboot4-files
```

本地存储不会暴露静态目录。`/api/admin/file/{id}/access-url` 会返回短期签名 URL，实际下载入口为 `/api/admin/file/access/{token}`，该入口在安全白名单中，但会校验 HMAC token 与过期时间。

权限边界按业务场景区分：

- 系统文件管理页使用 `/api/admin/file/**`，下载/预览需要 `sys:file:download`。
- 当前用户头像上传使用 `/api/admin/user/avatar/upload`，接口会直接写回当前用户 `profileFileId` 并返回头像访问 URL。
- CMS 封面和附件使用 `/api/admin/cms/file/upload` 与 `/api/admin/cms/file/{id}/access-url`，权限归属 `cms:file:upload`。

前端不得再拼接 `endpoint/bucket/objectKey` 作为文件直链，必须通过对应业务接口或系统文件管理接口获取访问 URL。

物理删除文件后不再复活软删除的 `sys_file` 记录。若数据库存在仅基于 `file_hash` 的全局唯一索引，应调整为只约束未删除记录，或直接删除该唯一索引，避免同 hash 文件重新上传时撞库。

## 8. Excel

Excel 不再从 Entity 注解上承载所有语义，标准做法是：

- 导出使用独立 `ExportRow`。
- 导入使用独立 `ImportRow`。
- `BaseCrudController` 默认导出 `ExportRow`。
- `/import` 默认关闭，只有 Controller 显式开启导入能力时才允许访问。
- Entity 上历史 Excel 注解应逐步迁移到 Row 类。

## 9. OpenAPI 分组

Controller 应同时声明：

```java
@Tag(name = "用户管理", description = "SysUser - 用户管理")
@ApiTagGroup(group = "系统管理")
```

应用启动时会扫描 `@ApiTagGroup` 并生成 Scalar/OpenAPI 分组。更多细节见 [OpenAPI 标签分组使用指南](API_TAG_GROUP_GUIDE.md)。

## 10. 低代码生成契约

新增业务资源建议先准备元数据：

```yaml
domain: sys
resource: user
entity: SysUser
basePath: /api/admin/user
permissionPrefix: sys:user
menu:
  parentId: sys_menu_system
  title: system.user.title
  icon: carbon:user
```

生成内容应包含：

- DTO/VO/ExcelRow
- MapStruct mapper 或低风险默认 mapper
- Service/Controller
- 权限码
- Flyway 迁移脚本
- 前端 API
- vben 列表页、表单抽屉、权限按钮

详细契约见 [CRUD Module Generator Contract](crud_module_generator_contract.md)。

## 11. 测试与架构约束

后端回归：

```powershell
.\gradlew.bat compileJava test
```

前端回归：

```powershell
pnpm -F @vben/web-antd typecheck
```

本地后端启动后执行接口冒烟：

```powershell
.\scripts\admin-smoke.ps1 -BaseUrl "http://localhost:8080" -Username admin -Password admin123
```

需要验证通用 CRUD 写入链路时追加 `-Crud`，脚本会创建并清理临时字典数据。

架构约束：

- 生产代码 `extends BaseController` 数量必须为 0。
- 高风险模块禁止使用 `IdentityCrudMapper`。
- DTO/VO 不允许出现 `@Table`、`@Column`、`@Relation*`。
- Controller 不直接返回 Entity。
- 自定义 Admin 接口必须有明确权限声明。

## 12. 新模块接入清单

1. 明确模块元数据：`domain`、`resource`、`basePath`、`permissionPrefix`。
2. 建 Entity 和 Flyway 表结构迁移。
3. 建 `CreateReq`、`UpdateReq`、`ListVO`、`DetailVO`。
4. 高风险模块建 MapStruct `CrudMapper`。
5. 建 Controller，继承 `BaseCrudController` 或 `EntityCrudController`。
6. 配置 `CrudFieldPolicy`。
7. 配置 Excel `ExportRow`，必要时启用 `ImportRow`。
8. 写菜单和按钮权限 Flyway 迁移。
9. 写前端 API、列表页、表单页。
10. 跑后端和前端回归。
