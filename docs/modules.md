# 模块文档索引

本文档是业务模块文档的统一入口。模块自己的 README 或专项文档保留在模块目录内，便于贴近代码维护；这里负责聚合导航和说明模块边界。

## 核心与公共底座

| 模块 | 定位 | 文档 |
| --- | --- | --- |
| `flexboot4-core` | 纯 Java 基础契约、DTO、工具 | 暂无独立文档 |
| `flexboot4-admin-kernel` | Starter 公共底座，CRUD、查询、转换、Excel、上下文 | [开发者指南](guide.md) |
| `flexboot4-bom` | 版本约束与依赖管理 | [Starter 架构](STARTER_ARCHITECTURE.md) |

## Admin

| 模块 | 定位 | 文档 |
| --- | --- | --- |
| `flexboot4-admin-starter` | RBAC、用户、角色、菜单、配置、文件、操作日志、API Key | [Admin Starter README](../flexboot4-admin-starter/README.md) |

相关专题：

- [权限设计](backend_permission_control_design.md)
- [Admin 认证与账号安全](admin-auth-security.md)
- [OpenAPI 标签分组](API_TAG_GROUP_GUIDE.md)
- [通用查询](Mf基础功能.md)
- [CRUD 生成契约](crud_module_generator_contract.md)

## 业务 Starter

| 模块 | 定位 | 文档 |
| --- | --- | --- |
| `flexboot4-cms-starter` | CMS 栏目、文章、模板、静态发布 | [CMS README](../flexboot4-cms-starter/README.md) |
| `flexboot4-media-starter` | ZLMediaKit、GB28181、设备通道、分屏、国标级联 | [Media README](../flexboot4-media-starter/README.md) |
| `flexboot4-sms4j-starter` | sms4j 厂商配置、动态刷新、短信能力 | [SMS4J Starter](../flexboot4-sms4j-starter/SMS4J_STARTER.md) |
| `flexboot4-kb-starter` | 知识库、文件解析、RAG 扩展能力 | 暂无独立 README |

Media 专项文档：

- [Media 计划](../flexboot4-media-starter/Media-PLAN.md)
- [Media 集成检查清单](../flexboot4-media-starter/MEDIA-INTEGRATION-CHECKLIST.md)

## 独立服务

| 模块 | 定位 | 文档 |
| --- | --- | --- |
| `flexboot4-ai` | AI Gateway，WebFlux 流式代理、API Key 离线鉴权、调用日志汇聚 | [AI 结构说明](../flexboot4-ai/struc.md) |

## 内部开发应用

| 模块 | 定位 | 文档 |
| --- | --- | --- |
| `flexboot4-bootstrap` | 聚合所有 starter 的本仓库联调应用 | [开发者指南](guide.md) |

## SQL 与初始化

SQL 脚本跟随业务 starter 维护，不再集中放在 `docs` 下。每个需要数据库资源的模块在 `src/main/resources/db/` 下提供：

- `init.sql`：该模块的业务表、索引和必要初始化数据
- `menu_data.sql`：该模块的菜单、按钮和权限初始化数据
- `migration/flexboot4/{module}/postgresql/V*__*.sql`：后续功能演进的 Flyway 脚本

当前模块脚本入口：

- `flexboot4-admin-starter/src/main/resources/db/init.sql`
- `flexboot4-admin-starter/src/main/resources/db/menu_data.sql`
- `flexboot4-cms-starter/src/main/resources/db/init.sql`
- `flexboot4-cms-starter/src/main/resources/db/menu_data.sql`
- `flexboot4-media-starter/src/main/resources/db/init.sql`
- `flexboot4-media-starter/src/main/resources/db/menu_data.sql`
- `flexboot4-sms4j-starter/src/main/resources/db/init.sql`
- `flexboot4-sms4j-starter/src/main/resources/db/menu_data.sql`
- `flexboot4-kb-starter/src/main/resources/db/init.sql`
- `flexboot4-kb-starter/src/main/resources/db/menu_data.sql`

开发阶段旧数据不要求兼容。若数据不符合当前契约，优先使用模块 SQL 或 Flyway 迁移修正数据，而不是在代码里保留旧兼容逻辑。
