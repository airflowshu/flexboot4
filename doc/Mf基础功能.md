# 通用查询构建与使用说明

## api应用场景：**单表** 或 **多表关联** 查询`/list`、`/page`

## 基础查询参数格式
- 入参对象为 SearchDto，包含：
  - pageNumber、pageSize
  - items：支持递归 children 的条件列表，children 之间使用 logic 组合（AND/OR），根层使用 dto.logic 组合
  - orders：多字段排序，column 为实体属性或关联路径，asc 为方向
- 空值忽略：当条件值为 null 时自动忽略该条件

约定（推荐用法）

- SearchItem.field 的前缀必须是“业务表名/实体名的驼峰写法”，例如：
    - 表 iot_device → iotDevice.xxx
    - 表 sys_dict_type → sysDictType.code

单表示例：
```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "logic": "AND",
  "items": [
    { "field": "status", "op": "eq", "val": 1 },
    {
      "logic": "OR",
      "children": [
        { "field": "username", "op": "like", "val": "mi" },
        { "field": "realName", "op": "like", "val": "mi" }
      ]
    }
  ],
  "orders": [
    { "column": "createTime", "asc": false }
  ]
}
```

## 关联查询语法规范
- 字段路径采用点号表示：relation.property
- relation 为实体上声明的关联属性名，支持 @RelationManyToOne、@RelationManyToMany
- 根据注解自动构建 left join 及 on 条件
- 排序可使用关联列，如 "dept.deptName"、"roles.roleName"；亦可以使用表名驼峰格式，如"sysDept.deptName"、"sysRole.roleName"

关联示例：
```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "items": [
    { "field": "dept.deptName", "op": "like", "val": "研发" },
    { "field": "sysUser.createTime", "op": "gt", "val": "2026-01-01 00:00:00" },
    { "field": "roles.roleValue", "op": "eq", "val": "admin" }
  ],
  "orders": [
    { "column": "dept.deptName", "asc": true }
  ]
}
```

## 特殊查询场景示例
- in/notin 值支持数组、集合或逗号分隔字符串：
  - { "field": "id", "op": "in", "val": ["1","2","3"] }
  - { "field": "id", "op": "in", "val": "1,2,3" }
- isnull/notnull 用于空值判断：
  - { "field": "email", "op": "isnull" }
  - { "field": "avatar", "op": "notnull" }
- 嵌套条件分组：
  - children + logic 组合复杂 AND/OR 场景

## 自定义扩展方式说明
- 模板方法扩展：
  - AbstractQueryWrapperBuilder 定义构建流程，业务可继承并重载列解析、操作符策略、默认排序等
- 注解扩展：
  - 可为实体属性增加自定义注解如 @QueryField/@QueryIgnore 用于别名、禁用操作符、值转换等（建议在代码生成阶段同步生成处理）
- 配置扩展：
  - 在 src/main/resources/query-rules.yml 中定义全局或实体级规则，实现特殊字段映射、操作符权限、默认排序等

## 与 MyBatis-Flex 行为一致性
- 空值条件自动忽略
- 关联查询通过 left join 构建，支持 QueryWrapper 的 and/or 分组
- 排序支持多列与关联列

## 代码位置
- BaseController：通用方法入口 [BaseController.java](../src/main/java/com/yunlbd/flexboot4/controller/BaseController.java)
- 默认构建器：DefaultQueryWrapperBuilder [DefaultQueryWrapperBuilder.java](../src/main/java/com/yunlbd/flexboot4/query/DefaultQueryWrapperBuilder.java)
- 关联解析：RelationQueryBuilder [RelationQueryBuilder.java](../src/main/java/com/yunlbd/flexboot4/query/RelationQueryBuilder.java)
- 工具：FieldResolver、ValueConverter、OperatorStrategies、SearchDtoUtils


## 字典设计要点

* 使用已有的静态解析器 [DictTextResolver](../src/main/java/com/yunlbd/flexboot4/excel/DictTextResolver.java) 作为统一入口，避免监听器直接依赖 Spring Bean。

* 通过在实体字段上标注字典类型（使用注解 [ExcelDict](../src/main/java/com/yunlbd/flexboot4/excel/ExcelDict.java)），[GlobalDictSetListener.java](../src/main/java/com/yunlbd/flexboot4/listener/GlobalDictSetListener.java)全局监听器在 onSet 时读取注解决定字典类型（无需在具体的业务类上的@Table注解内增加配置onSet）。
> e.g. 更新 SysUser.java 去除 @Table(onSet=...)，改用全局监听器；性别字段标注 @ExcelDict("gender") 保持回写到 genderStr

* 监听器通用化：只要字段带有 @ExcelDict("<typeCode>") **声明取注解的字典类型 code**，就将解析得到的文本写入同名的 <fieldName>Str 字段。
* 对于未标注的字段，维持现状，不做字典回写。
* 未匹配字典项：返回原始 code 字符串，便于排查。
* 顺序：typeHandler 优先于 SetListener，符合 MyBatis-Flex 官方说明。
* 全局 onSet 能力：新增全局监听器，对所有继承 BaseEntity 的实体启用字典回写 
* 多字典字段支持：按每次属性赋值触发，逐字段检测并解析，支持同一实体含多个字典字段

# 邮件配置

```yaml
spring:
    mail:
    host: smtp.example.com        # SMTP 服务器地址
    port: 587                     # SMTP 端口 (TLS)
    username: your-email@example.com
    password: your-app-password   # 或授权码
    properties:
        mail:
            smtp:
                auth: true
                starttls:
                 enable: true

# 自定义邮件配置
mail:
    host: ${spring.mail.host}
    port: ${spring.mail.port}
    username: ${spring.mail.username}
    password: ${spring.mail.password}
    sender-name: "FlexBoot4"        # 发件人显示名称
    reset-url: "http://localhost:3000/reset-password"  # 前端重置密码页面
    token-expiration-minutes: 30    # 重置链接有效期(分钟)
```
## 生产配置：
生产环境的 reset-url 应该是前端实际部署的域名：

mail:
    reset-url: https://your-frontend-domain.com/auth/reset-password

> 
    常用邮箱配置参考：
    ┌────────────┬────────────────────┬─────────┐
    │ 邮箱服务商 │    SMTP 服务器     │  端口   │
    ├────────────┼────────────────────┼─────────┤
    │ Gmail      │ smtp.gmail.com     │ 587     │
    ├────────────┼────────────────────┼─────────┤
    │ QQ 邮箱    │ smtp.qq.com        │ 465/587 │
    ├────────────┼────────────────────┼─────────┤
    │ 163 邮箱   │ smtp.163.com       │ 465/994 │
    ├────────────┼────────────────────┼─────────┤
    │ 企业邮箱   │ smtp.exmail.qq.com │ 465     │
    └────────────┴────────────────────┴─────────┘
    注意： Gmail 和 QQ 邮箱需要使用应用专用密码，而不是登录密码。


如何运行

- 启动后台管理（MVC）： .\gradlew.bat :admin-server:bootRun
- 启动 AI 中台（WebFlux）： .\gradlew.bat :ai-gateway:bootRun
- 全量测试（根目录）： .\gradlew.bat test （已验证通过）


---
文件上传-----数据流

【解析阶段】admin-server
FileChunkingServiceImpl.chunk() 保存 chunk
→ publishChunks(entities) 批量发布
→ FileEmbeddingPublisherImpl 写入 Redis Stream
→ 每条消息: {chunkId, fileId, model, retryCount}

【向量阶段】ai-gateway
EmbeddingStreamStartupListener 启动消费者
→ XREADGROUP 消费消息
→ 查询 sys_file_chunk (embed_status=PENDING)
→ 更新状态 PROCESSING
→ 调用本地 Embedding HTTP 服务
→ 写入 ai_vector_chunk (ON CONFLICT DO UPDATE)
→ 更新状态 EMBEDDED / FAILED
→ 超过重试次数 → 写入 DLQ

---
