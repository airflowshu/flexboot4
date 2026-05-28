# 通用查询构建与使用说明

FlexBoot4 通用 CRUD 的 `/page`、`/list`、`/export` 使用 `SearchDto` 描述查询条件。查询构建器基于 MyBatis-Flex `QueryWrapper`，支持单表、根表别名、关系字段、嵌套条件和多字段排序。

## 1. SearchDto 结构

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "logic": "AND",
  "items": [],
  "orders": []
}
```

字段说明：

| 字段 | 说明 |
| --- | --- |
| `pageNumber` | 页码，默认 1 |
| `pageSize` | 每页数量，默认 10 |
| `logic` | 根条件组合方式，`AND` 或 `OR` |
| `items` | 查询条件，支持递归 `children` |
| `orders` | 排序条件 |

## 2. 单表查询

裸字段写法：

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "items": [
    { "field": "status", "op": "eq", "val": 1 },
    { "field": "realName", "op": "like", "val": "张" }
  ],
  "orders": [
    { "column": "createTime", "asc": false }
  ]
}
```

根表别名写法：

```json
{
  "items": [
    { "field": "sysUser.status", "op": "eq", "val": 1 }
  ],
  "orders": [
    { "column": "sysUser.createTime", "asc": false }
  ]
}
```

根表别名可以使用：

- 实体类 lowerCamel：`sysUser.createTime`
- 表名 lowerCamel：`sysUser.createTime`
- 裸字段：`createTime`

## 3. 关系查询

关系字段使用点号：

```text
relation.property
```

`relation` 可以是：

- Entity 上的关系属性名，如 `dept.deptName`
- 目标实体 lowerCamel，如 `sysDept.deptName`
- 目标表名 lowerCamel，如 `sysDept.deptName`

示例：

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "items": [
    { "field": "dept.deptName", "op": "like", "val": "研发" },
    { "field": "roles.roleValue", "op": "eq", "val": "admin" }
  ],
  "orders": [
    { "column": "dept.deptName", "asc": true }
  ]
}
```

关系字段必须通过 Controller 的 `CrudFieldPolicy` 显式开放。即使根表允许 `createTime`，也不代表自动允许 `dept.createTime`。

## 4. 嵌套条件

```json
{
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
  ]
}
```

## 5. 操作符

常用操作符：

| op | 说明 |
| --- | --- |
| `eq` | 等于 |
| `ne` | 不等于 |
| `gt` | 大于 |
| `ge` | 大于等于 |
| `lt` | 小于 |
| `le` | 小于等于 |
| `like` | 模糊匹配 |
| `notlike` | 非模糊匹配 |
| `in` | 包含 |
| `notin` | 不包含 |
| `isnull` | 为空 |
| `notnull` | 不为空 |

`in` 和 `notin` 支持数组、集合或逗号分隔字符串：

```json
{ "field": "id", "op": "in", "val": ["1", "2", "3"] }
```

```json
{ "field": "id", "op": "in", "val": "1,2,3" }
```

## 6. 字段白名单

`BaseCrudController` 会在构建查询前调用 `CrudFieldPolicy`：

```java
@Override
protected CrudFieldPolicy fieldPolicy() {
    return CrudFieldPolicy.same(List.of(
            "id", "username", "realName", "email", "status", "createTime"
    )).withQueryFields("dept.deptName", "roles.roleValue");
}
```

规则：

- `same(...)` 同时开放查询和排序。
- `withQueryFields(...)` 只开放查询。
- `withOrderFields(...)` 只开放排序。
- 根表别名字段会规范化为裸字段后校验。
- 关系字段会规范化为关系属性名后校验。
- 未在白名单中的字段会抛出 `查询字段不允许` 或 `排序字段不允许`。

## 7. 与 DTO/VO 的关系

查询字段对应 Entity 字段和关系路径，不对应 VO 字段。

原因是查询发生在数据库层，而 VO 是响应契约。Controller 负责通过 `CrudFieldPolicy` 控制哪些 Entity 字段允许暴露为查询能力。

## 8. 代码位置

| 能力 | 文件 |
| --- | --- |
| CRUD 入口 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/controller/sys/BaseCrudController.java` |
| 字段白名单 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/controller/sys/CrudFieldPolicy.java` |
| 默认查询构建器 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/query/DefaultQueryWrapperBuilder.java` |
| 关系解析 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/query/RelationQueryBuilder.java` |
| 字段解析 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/query/FieldResolver.java` |
| 值转换 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/query/ValueConverter.java` |
| 查询工具 | `flexboot4-admin-kernel/src/main/java/com/yunlbd/flexboot4/query/SearchDtoUtils.java` |

## 9. 常见问题

### 排序字段不允许: sysUser.createTime

说明 Controller 的字段白名单没有通过实体上下文校验，或当前资源没有开放 `createTime`。标准 `BaseCrudController` 会将 `sysUser.createTime` 识别为根表 `createTime`。

### 关系字段查询报错

检查：

- Entity 是否声明了 `@RelationManyToOne`、`@RelationOneToMany`、`@RelationManyToMany` 或 `@RelationOneToOne`。
- Controller 是否在 `CrudFieldPolicy` 中显式开放该关系字段。
- 前缀是否能匹配关系属性名、目标实体名或目标表名。

### 返回数据里关系对象过多

这是 VO 设计问题。ListVO 不需要的关系字段不要声明；低风险默认映射器会忽略 Entity 多余字段，高风险模块应使用 MapStruct 精确控制输出。
