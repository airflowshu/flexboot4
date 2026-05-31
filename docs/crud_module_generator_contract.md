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

## 3. Service、缓存与 MyBatis-Flex 风格

生成的业务模块应优先遵循项目现有的通用 CRUD + Redis/Spring Cache 机制。除非业务语义确实需要特殊处理，不应绕过 `BaseServiceImpl` 提供的通用入口。

### 3.1 Service 基础形态

- `{Entity}Service` 继承 `IExtendedService<Entity>`。
- `{Entity}ServiceImpl` 继承 `BaseServiceImpl<{Entity}Mapper, Entity>` 并实现 `{Entity}Service`。
- `{Entity}ServiceImpl` 必须声明 `@Service` 和 `@CacheConfig(cacheNames = "...")`。
- `cacheNames` 使用稳定、唯一、可读的业务名，例如 `sysUser`、`cmsArticle`、`mediaChannel`。
- Mapper 默认只继承 `BaseMapper<Entity>`，保持空接口：

```java
@Mapper
public interface XxxMapper extends BaseMapper<Xxx> {
}
```

### 3.2 CRUD 缓存入口

- 标准新增、修改、删除、查询、分页、计数应调用 Service 通用 CRUD 方法，让 `BaseServiceImpl` 的 `@Cacheable` / `@CacheEvict` 和表版本号机制生效。
- ServiceImpl 内部调用通用 CRUD 时，应使用 `cacheProxy()`，避免 Spring AOP 自调用绕过缓存代理。
- 不应在业务方法中直接使用 `this.save(...)`、`this.updateById(...)`、`super.getById(...)`、`super.list(...)`、`super.page(...)` 等方式完成标准 CRUD。
- 子类覆盖 `save`、`updateById`、`removeById` 等父类 CRUD 方法时，必须保留缓存失效注解，例如：

```java
@Override
@CacheEvict(allEntries = true, cacheResolver = "dynamicCacheResolver")
public boolean save(@NonNull Xxx entity) {
    // custom logic
    return super.save(entity);
}
```

- 覆盖带实体参数的父类方法时，应保持父接口/父类的空值契约，参数补充 `org.jspecify.annotations.NonNull`。
- 手写 `UpdateChain`、直接 Mapper 更新或其他绕过通用写入口的逻辑，应在写方法上声明 `@BumpTableVersion(Entity.class)`；切面会在方法正常返回且布尔结果为 `true` 时自动推进表版本号。
- 如果写方法需要按表名失效缓存而不是按 Entity 推断表名，可使用 `@BumpTableVersion(tables = "sys_file")`。
- 被 `@BumpTableVersion` 标记的方法必须经由 Spring 代理调用；同一 ServiceImpl 内部调用时，应抽到接口方法并通过 `serviceProxy(XxxService.class)` 调用，避免自调用绕过 AOP。

### 3.3 查询与更新写法

- 常规查询使用 MyBatis-Flex `QueryWrapper`。
- 表字段优先使用 APT 生成的 `TableDef`，例如 `SysUserTableDef.SYS_USER.ID.eq(id)`。
- 避免在业务 Mapper 方法上新增 `@Select`、`@Update`、`@Insert`、`@Delete` 注解 SQL。
- 需要局部更新、字段置空、原子递增时，优先使用 `UpdateChain`：

```java
@Override
@BumpTableVersion(Xxx.class)
public boolean updateName(String id, String name) {
    XxxTableDef t = XxxTableDef.XXX;
    return UpdateChain.of(getMapper())
            .set(t.NAME, name, true)
            .where(t.ID.eq(id))
            .and(t.DEL_FLAG.eq(0))
            .update();
}
```

如果被注解方法仅能指定表名：

```java
@BumpTableVersion(tables = "xxx_table")
public boolean refreshSomething(String id) {
    XxxTableDef t = XxxTableDef.XXX;
    return UpdateChain.of(getMapper())
            .set(t.NAME, name, true)
            .where(t.ID.eq(id))
            .and(t.DEL_FLAG.eq(0))
            .update();
}
```

- `setRaw` 仅用于数据库原子表达式等明确场景，例如 `coalesce(view_count, 0) + 1`。
- 关系查询优先通过通用查询入口取得数据后使用 `RelationManager.queryRelations(...)` 装载关系；确有复杂投影或树形/RBAC 查询时，可保留 MyBatis-Flex Mapper 查询，但必须使用 `QueryWrapper` / APT 风格。

### 3.4 允许的特例

以下场景可以不强行改成通用 CRUD，但应在代码中保持边界清晰：

- 需要查询逻辑删除数据以恢复记录，例如成员恢复场景。
- 重建 Redis 派生缓存、快照、索引等需要数据库全量最新数据的任务。
- 菜单树、权限码、复杂投影、跨表 RBAC 查询等非实体 CRUD 语义。
- MFA、安全凭据、运行时状态等特殊安全/运行时流程；如果存在独立实体表，优先补一个薄的通用 Service 承接 CRUD。

特例仍应尽量使用 MyBatis-Flex `QueryWrapper` / APT `TableDef` / `UpdateChain`，不要退回注解 SQL。

### 3.5 跨 Service 依赖

- 一个业务 Service 需要读取或更新另一个聚合时，优先依赖对方 Service，而不是直接注入对方 Mapper。
- 如果 Service 之间形成启动期循环依赖，应优先通过 `@Lazy` 延迟其中一侧依赖，不要打开 `spring.main.allow-circular-references`。
- 只有在性能、逻辑删除恢复、快照重建等明确特例下，才允许直接使用本模块 Mapper。

## 4. API 规则

- Request 不包含 `id`、`version`、`delFlag`、`createTime`、`lastModifyTime`、`createBy`、`lastModifyBy`。
- Request 不包含关系对象。
- Response 使用 ListVO/DetailVO，不直接返回 Entity。
- 查询和排序字段通过 `CrudFieldPolicy`。
- Excel 导出使用 `ExportRow`。
- Excel 导入默认关闭，只有声明 `ImportRow` 且 Controller 显式开启时才生成 `/import` 可用逻辑。

## 5. 权限码

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

## 6. vben 菜单 SQL

菜单生成应包含：

- catalog/menu 路由节点。
- button 权限节点。
- vben meta 字段：`hideInMenu`、`hideInTab`、`hideInBreadcrumb`。
- `authCode` 与权限码一致。
- 根节点 `parent_id` 使用 `NULL`。
- 布局组件使用前端 `layoutMap` 支持的值。

button 节点不进入后端动态路由树，只进入权限码接口。

## 7. 前端生成内容

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

## 8. 架构测试契约

生成后应满足：

- 生产代码不出现 `extends BaseController`。
- 高风险模块不使用 `IdentityCrudMapper`。
- DTO/VO 不出现 `@Table`、`@Column`、`@Relation*`。
- Controller 不直接暴露 Entity 作为请求或响应契约。
- 业务 Mapper 不新增注解 SQL：`@Select`、`@Update`、`@Insert`、`@Delete`。
- ServiceImpl 内部标准 CRUD 不通过 `this.*` 或 `super.*` 绕过缓存代理。
- 覆盖父类 CRUD 方法时保留缓存注解和 `@NonNull` 参数契约。

## 9. 回归命令

```powershell
.\gradlew.bat compileJava test
pnpm -F @vben/web-antd typecheck
rg -n "@Select|@Update|@Insert|@Delete" -S -g "*.java"
rg -n "super\.(getById|list|page|getOne|count|save|updateById|removeById|removeByIds|remove\(|update\()|this\.(save|saveBatch|updateById|removeById|remove\()" -S -g "*.java"
```

如扫描存在命中，应确认是否属于 `BaseServiceImpl` 内部实现、Controller 映射注解、或已说明的业务特例。

## 10. 模板目录

当前契约配套模板位于 `templates/crud/`：

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
