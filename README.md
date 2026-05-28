# FlexBoot4

FlexBoot4 是一个基于 Spring Boot 4、Java 25、MyBatis-Flex 和 PostgreSQL 的模块化后台与 AI 能力聚合工程。当前工程采用 Starter 化设计：通用底座、Admin/RBAC、CMS、Media、SMS、KB 与 AI Gateway 按模块拆分，业务应用可以按需组合。

本仓库仍处于开发演进阶段，当前后端 CRUD、权限与前端 vben 对接已经切换到新的设计范式：

- API 契约不再直接暴露 Entity，统一使用 `CreateReq`、`UpdateReq`、`ListVO`、`DetailVO`。
- 通用 CRUD 入口为 `BaseCrudController` / `EntityCrudController`，旧 `BaseController` 不再作为生产代码入口。
- 高风险模块使用显式 DTO/VO/ExcelRow/MapStruct，普通基础表可使用低代码默认模板。
- `/api/admin/**` 默认拒绝未声明权限的接口，CRUD 权限码由 `BaseCrudController` 自动推导。
- vben 后端路由接口只返回路由树，按钮权限码通过 `/admin/auth/codes` 进入前端。

## 文档入口

新开发者建议按下面顺序阅读：

| 文档 | 说明 |
| --- | --- |
| [开发者指南](docs/guide.md) | 当前框架范式、CRUD、权限、vben 菜单路由、Excel 与测试约束 |
| [模块文档索引](docs/modules.md) | Admin、CMS、Media、SMS、KB、AI 等业务模块入口 |
| [Starter 架构](docs/STARTER_ARCHITECTURE.md) | 模块依赖边界与外部项目集成方式 |
| [CRUD 生成契约](docs/crud_module_generator_contract.md) | 后续沉淀为低代码生成器或 Codex Skill 的模板契约 |
| [权限设计](docs/backend_permission_control_design.md) | 当前 RBAC、权限码推导、默认拒绝策略 |
| [通用查询](docs/Mf基础功能.md) | `SearchDto`、关联查询、字段白名单与排序规则 |
| [OpenAPI 分组](docs/API_TAG_GROUP_GUIDE.md) | `@ApiTagGroup` 使用规范 |

## 模块结构

```text
flexboot4
├── flexboot4-core              # 纯 Java 基础契约、DTO、工具
├── flexboot4-admin-kernel      # Starter 公共底座：CRUD 基座、查询、转换、Excel、上下文
├── flexboot4-admin-starter     # Admin/RBAC/系统管理/运维管理
├── flexboot4-cms-starter       # CMS 内容管理与静态发布
├── flexboot4-media-starter     # Media/ZLMediaKit/GB28181/分屏/级联
├── flexboot4-sms4j-starter     # 短信厂商配置与 sms4j 动态刷新
├── flexboot4-kb-starter        # 知识库与 RAG 扩展能力
├── flexboot4-ai                # AI Gateway，独立 WebFlux 服务
├── flexboot4-bootstrap         # 内部开发聚合应用
└── docs                        # 开发文档、SQL、模块入口
```

## 技术栈

| 类别 | 技术 |
| --- | --- |
| Runtime | Java 25 |
| Framework | Spring Boot 4 |
| ORM | MyBatis-Flex |
| Database | PostgreSQL |
| Cache/MQ | Redis / Redis Stream |
| Auth | Spring Security + JWT |
| API Docs | SpringDoc + Scalar |
| Frontend | vue-vben-admin 5.7.x |
| Build | Gradle Kotlin DSL |

## 本地开发

后端聚合应用：

```powershell
.\gradlew.bat :flexboot4-bootstrap:bootRun
```

后端编译与测试：

```powershell
.\gradlew.bat compileJava test
```

前端类型检查：

```powershell
pnpm -F @vben/web-antd typecheck
```

API 文档默认入口：

```text
http://localhost:8080/scalar
```

## 外部项目集成

推荐通过 BOM 统一版本：

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    implementation("com.yunlbd:flexboot4-admin-starter")
    // implementation("com.yunlbd:flexboot4-cms-starter")
    // implementation("com.yunlbd:flexboot4-media-starter")
    // implementation("com.yunlbd:flexboot4-sms4j-starter")
    // implementation("com.yunlbd:flexboot4-kb-starter")
}
```

业务应用不需要配置 `scanBasePackages("com.yunlbd.flexboot4")`。Starter 通过 Spring Boot 自动装配加载框架 Bean；业务自己的 Mapper 只扫描业务包即可。

## 开发约束

生产代码应遵守以下约束：

- 不继承旧 `BaseController`。
- Controller 返回 VO，不直接返回 Entity。
- 请求 DTO 不包含主键、审计字段、逻辑删除字段和关系对象。
- 高风险模块禁止使用 `IdentityCrudMapper`。
- 查询与排序字段必须通过 `CrudFieldPolicy` 白名单。
- Excel 导出使用 `ExportRow`，导入默认关闭，按模块显式开启。
- `/api/admin/**` 自定义接口必须显式声明 `@RequirePermission` 或 `skip = true`。

更多细节见 [开发者指南](docs/guide.md)。
