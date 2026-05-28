# FlexBoot4 文档中心

欢迎来到 FlexBoot4 文档中心。当前工程已经完成新的后端框架范式调整，开发时请优先阅读下面几篇文档。

## 推荐阅读顺序

1. [开发者指南](guide.md)
2. [模块文档索引](modules.md)
3. [Admin 认证与账号安全](admin-auth-security.md)
4. [Starter 架构](STARTER_ARCHITECTURE.md)
5. [权限设计](backend_permission_control_design.md)
6. [通用查询](Mf基础功能.md)
7. [CRUD 生成契约](crud_module_generator_contract.md)

## 核心主题

| 主题 | 文档 |
| --- | --- |
| 新 CRUD 范式 | [开发者指南](guide.md) |
| DTO/VO/MapStruct/ExcelRow | [开发者指南](guide.md) |
| 权限码与默认拒绝策略 | [权限设计](backend_permission_control_design.md) |
| 登录、个人中心安全设置与 MFA | [Admin 认证与账号安全](admin-auth-security.md) |
| SearchDto 与关联查询 | [通用查询](Mf基础功能.md) |
| vben 菜单路由契约 | [开发者指南](guide.md) |
| OpenAPI 标签分组 | [OpenAPI 标签分组](API_TAG_GROUP_GUIDE.md) |
| 低代码生成模板 | [CRUD 生成契约](crud_module_generator_contract.md) |
| 业务模块入口 | [模块文档索引](modules.md) |

## 模块入口

- [Admin Starter](../flexboot4-admin-starter/README.md)
- [CMS Starter](../flexboot4-cms-starter/README.md)
- [Media Starter](../flexboot4-media-starter/README.md)
- [SMS4J Starter](../flexboot4-sms4j-starter/SMS4J_STARTER.md)
- [AI Gateway](../flexboot4-ai/struc.md)

## SQL

SQL 脚本集中在 [sql](sql/) 目录。当前项目处于开发阶段，旧数据不符合新契约时优先用 SQL 修正，不保留旧兼容逻辑。
