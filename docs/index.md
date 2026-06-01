---
layout: home

hero:
  name: FlexBoot4
  text: 前后端分离开发框架
  tagline: 面向模块化 Starter、权限治理、数据库迁移和 vben 前端集成的工程文档。
  actions:
    - theme: brand
      text: 快速开始
      link: /QUICKSTART
    - theme: alt
      text: 开发者指南
      link: /guide
    - theme: alt
      text: GitHub
      link: https://github.com/airflowshu/flexboot4

features:
  - title: 模块化 Starter
    details: Admin、CMS、Media、SMS4J、KB 等能力以 Starter 形式组合，适合按需引入和独立演进。
    link: /modules
    linkText: 查看模块索引
  - title: 数据库迁移治理
    details: 内置模块通过 Flyway 目录声明统一聚合迁移，开发期重置和新模块接入更清晰。
    link: /database-migration
    linkText: 了解 Flyway 规则
  - title: 权限与认证安全
    details: 覆盖登录认证、个人中心安全、MFA、权限码推导和默认拒绝策略。
    link: /admin-auth-security
    linkText: 查看安全专题
  - title: CRUD 与查询契约
    details: 规范 DTO/VO、MapStruct、Excel、SearchDto、字段白名单和低代码生成模板。
    link: /crud_module_generator_contract
    linkText: 阅读生成契约
  - title: 前端 Companion Package
    details: 后端 Starter 与前端业务包保持模块边界，统一菜单路由、API、语言包和启用机制。
    link: /guide
    linkText: 查看集成方式
  - title: 本地与远端文档发布
    details: 本地 push 前自动构建 VitePress，远端 GitHub Pages 自动部署，减少发布漂移。
    link: /FAQ
    linkText: 查看常见问题
---
