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

## 前端 Companion Package

前端与后端 starter 保持同样的模块边界。`flexboot-web/vue-vben-admin/apps/web-antd` 只作为宿主壳，保留登录、布局、请求、权限指令、路由守卫、菜单拉取、fallback 与通用组件适配；业务页面、业务 API 与模块语言包放在对应的 `packages/business/*` 包中。

| 后端 starter | 前端 companion package | 前端包目录 |
| --- | --- | --- |
| `flexboot4-admin-starter` | `@flexboot4/admin-web` | `packages/business/admin-web` |
| `flexboot4-cms-starter` | `@flexboot4/cms-web` | `packages/business/cms-web` |
| `flexboot4-media-starter` | `@flexboot4/media-web` | `packages/business/media-web` |
| `flexboot4-sms4j-starter` | `@flexboot4/sms4j-web` | `packages/business/sms4j-web` |
| `flexboot4-kb-starter` | `@flexboot4/kb-web` | `packages/business/kb-web` |

新项目选择后端能力时，需要同时声明对应前端包，并在宿主的 `apps/web-antd/src/modules/enabled.ts` 中注册启用。第一版采用 pnpm workspace 编译期集成，不做运行时远程加载。

前端模块契约：

- 后端 `menu_data.sql` 的 `component` 字段保持 `/module/page/index` 风格。
- 前端业务包通过 `@flexboot4/web-kit` 暴露 `pages`、`componentKeys`、`locales` 和可选 `install`。
- 宿主合并核心页面与 enabled modules 的页面映射后解析动态路由。
- 菜单管理的组件候选项来自当前 enabled modules，避免配置未集成模块的页面。
- 业务包 API 通过 `useFlexbootRequestClient()` 取得宿主注入的 request client，不反向依赖宿主 `#/api/request`。
- 后端菜单与前端页面的契约通过前端仓库 `pnpm check:flexboot-routes` 校验。

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
