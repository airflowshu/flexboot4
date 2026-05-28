# CRUD Module Generator Contract

本文档定义 FlexBoot4 CRUD 模块生成契约，可用于后续沉淀 `flexboot-crud-module-generator` Skill 或代码生成器。生成目标是：低代码可复用、DTO/VO 边界清晰、权限码和 vben 菜单自动对齐。

## 1. 元数据

```yaml
domain: sys
resource: user
entity: SysUser
basePath: /api/admin/user
permissionPrefix: sys:user
risk: high
menu:
  parentId: sys_menu_system
  title: system.user.title
  icon: carbon:user
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `domain` | 权限域和包分组，如 `sys`、`cms`、`media` |
| `resource` | 资源名，如 `user`、`article` |
| `entity` | Entity 类名 |
| `basePath` | Controller 路径 |
| `permissionPrefix` | 权限码前缀 |
| `risk` | `high` 或 `normal` |
| `menu` | vben 菜单元数据 |

## 2. 后端生成内容

```text
dto/{domain}/{Entity}CreateReq
dto/{domain}/{Entity}UpdateReq
vo/{domain}/{Entity}ListVO
vo/{domain}/{Entity}DetailVO
excel/{domain}/{Entity}ExportRow
excel/{domain}/{Entity}ImportRow       # 仅开启导入时生成
converter/{domain}/{Entity}CrudMapper  # high risk 必须生成
controller/{domain}/{Entity}Controller
service/{domain}/{Entity}Service
service/{domain}/impl/{Entity}ServiceImpl
mapper/{Entity}Mapper
```

高风险模块：

- Controller 继承 `BaseCrudController`。
- 必须使用 MapStruct `CrudMapper`。
- 必须显式声明 `CrudFieldPolicy`。
- 必须显式声明 `CrudExcelSupport`。

普通模块：

- Controller 可继承 `EntityCrudController<S,E,ID,CreateReq,UpdateReq,ListVO,DetailVO>`。
- 仍然必须生成 DTO/VO。
- 可以使用默认低代码映射器，但不能把 Entity 作为 API 契约。

## 3. API 规则

- Request 不包含 `id`、`version`、`delFlag`、`createTime`、`lastModifyTime`、`createBy`、`lastModifyBy`。
- Request 不包含关系对象。
- Response 使用 ListVO/DetailVO，不直接返回 Entity。
- 查询和排序字段通过 `CrudFieldPolicy`。
- Excel 导出使用 `ExportRow`。
- Excel 导入默认关闭，只有声明 `ImportRow` 且 Controller 显式开启时才生成 `/import` 可用逻辑。

## 4. 权限码

```text
{domain}:{resource}:list
{domain}:{resource}:add
{domain}:{resource}:edit
{domain}:{resource}:delete
{domain}:{resource}:export
{domain}:{resource}:import
```

自定义动作必须使用明确动作名，例如：

```text
sys:user:reset-password
cms:article:review
media:channel:play
```

## 5. vben 菜单 SQL

菜单生成应包含：

- catalog/menu 路由节点。
- button 权限节点。
- vben meta 字段：`hideInMenu`、`hideInTab`、`hideInBreadcrumb`。
- `authCode` 与权限码一致。
- 根节点 `parent_id` 使用 `NULL`。
- 布局组件使用前端 `layoutMap` 支持的值。

button 节点不进入后端动态路由树，只进入权限码接口。

## 6. 前端生成内容

```text
api/{domain}/{resource}.ts
views/{domain}/{resource}/list.vue
views/{domain}/{resource}/{resource}-drawer.vue
```

前端应生成：

- `CreateReq`
- `UpdateReq`
- `ListVO`
- `DetailVO`
- API client
- vben 列表页
- 表单抽屉或弹窗
- 权限按钮
- `SearchDto` 查询条件

按钮权限码必须与后端一致，例如新增按钮使用 `sys:user:add`，不要使用 `sys:user:create`。

## 7. 架构测试契约

生成后应满足：

- 生产代码不出现 `extends BaseController`。
- 高风险模块不使用 `IdentityCrudMapper`。
- DTO/VO 不出现 `@Table`、`@Column`、`@Relation*`。
- Controller 不直接暴露 Entity 作为请求或响应契约。

## 8. 回归命令

```powershell
.\gradlew.bat compileJava test
pnpm -F @vben/web-antd typecheck
```

## 9. 模板目录

当前契约配套模板位于 [templates/crud](templates/crud/)：

```text
templates/crud/metadata.yml
templates/crud/backend/Controller.explicit.java.tpl
templates/crud/backend/Controller.normal.java.tpl
templates/crud/backend/CrudMapper.java.tpl
templates/crud/backend/CreateReq.java.tpl
templates/crud/backend/UpdateReq.java.tpl
templates/crud/backend/ListVO.java.tpl
templates/crud/backend/DetailVO.java.tpl
templates/crud/backend/ExportRow.java.tpl
templates/crud/sql/menu.sql.tpl
templates/crud/frontend/api.ts.tpl
```

这些模板使用 `{{placeholder}}` 占位，后续可由 Skill、Gradle task 或独立代码生成器替换。
