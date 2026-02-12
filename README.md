# FlexBoot4 AI Platform

<div align="center">

![FlexBoot4 Logo](https://img.shields.io/badge/FlexBoot-4.0-blue?style=for-the-badge&logo=springboot)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![MyBatis-Flex](https://img.shields.io/badge/MyBatis--Flex-1.11.6-red?style=flat-square)](https://mybatis-flex.com/)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?style=flat-square&logo=gradle)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](./LICENSE)

**新一代企业级 AI 中台与后台管理聚合系统 | Spring Boot 4 + WebFlux + AI Gateway**

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [项目结构](#-项目结构) • [模块组装](#-模块组装) • [版本管理](#-版本管理) • [快速开始](#-快速开始)

</div>

## 📖 简介

**FlexBoot4** 是一个基于 Spring Boot 4 (Java 25) 构建的现代化多模块聚合工程。它创新性地将传统的 **Admin 后台管理** 与 **AI 智能网关** 进行架构分离，并通过模块化设计支持 **知识库 (RAG)** 能力的按需挂载。

核心理念：**Admin 负责 IAM 与规则定义，KB 负责知识资产沉淀，AI Gateway 负责高性能运行时执行。**

## ✨ 功能特性

### 🛡️ FlexBoot4 Admin (管理内核)
- **RBAC 权限系统**：基于 Spring Security + JWT，支持方法级权限控制 (`@RequirePermission`)。
- **动态分表审计**：操作日志 (`sys_oper_log`) 自动按季度分表 (`_YYYY_qN`)，支持跨季度查询。
- **数据脱敏**：内置脱敏工具，支持手机、身份证、银行卡等 8 种敏感数据自动脱敏。
- **API Key 管理**：全生命周期管理 AI 访问密钥，支持配额、模型权限与状态控制。

### 📚 FlexBoot4 KB (知识库扩展)
- **RAG 基础设施**：提供非结构化文档（PDF, Word, Excel）的解析与切片能力。
- **向量化流水线**：集成文件解析器与 Embedding 向量化任务调度。
- **按需加载**：作为独立模块设计，不使用知识库功能时无需引入重型依赖（如 PDFBox, POI）。

### 🤖 FlexBoot4 AI (智能网关)
- **高性能 WebFlux**：基于 Reactor 响应式编程，支持高并发 AI 请求转发。
- **API Key 离线鉴权**：基于 Snapshot + Cache 机制，无需 RPC 即可完成鉴权与配额校验。
- **统一日志汇聚**：通过 Redis Stream 将 AI 调用日志异步投递至 Admin Server 落库。
- **流式响应支持**：原生支持 SSE (Server-Sent Events) 流式数据透传。

## 🛠 技术栈

| 类别 | 技术框架                   | 说明 |
| --- |------------------------| --- |
| **Core** | Spring Boot 4.0.2      | 核心框架 (Java 25) |
| **ORM** | MyBatis-Flex 1.11.6    | 灵活的持久层框架 |
| **Database** | PostgreSQL + R2DBC     | 关系型数据库 (Admin: JDBC, AI: R2DBC) |
| **Cache/MQ** | Redis 7.x              | 缓存、限流与 Redis Stream 消息队列 |
| **Auth** | Spring Security + JJWT | 认证与授权 |
| **Build** | Gradle (Kotlin DSL)    | 构建工具，使用 Version Catalog |
| **Docs** | SpringDoc + Scalar     | API 文档与调试 UI |

## 📂 项目结构

```bash
flexboot4
├── flexboot4-admin         # [核心] 后台管理主服务 (RBAC, Audit, System)
│   └── 提供基础管理能力，对外暴露 API 依赖
├── flexboot4-ai            # [独立] AI 智能网关 (Spring WebFlux)
│   └── 独立部署，负责 AI 流量代理与计费
├── flexboot4-core          # [基座] 基础设施 (Pure Java)
│   └── DTO, Enums, Utils, 统一契约
├── flexboot4-kb            # [扩展] 知识库模块 (PDF/Word 解析, 向量化)
│   └── 依赖 flexboot4-admin，引入重型解析库
├── flexboot4-media         # [扩展] 媒体模块 (空壳扩展，可按需实现视频/音频能力)
│   └── 建议 api 依赖 flexboot4-admin，对外透传 admin 能力
├── flexboot4-bootstrap     # [示例] 启动入口 (Admin + KB 组装)
│   └── 演示如何将 Admin 与 KB 组装运行
└── gradle                  # Gradle Version Catalog
```

## 📦 模块组装

FlexBoot4 采用模块化架构，支持根据业务需求按需组装。原则是：**扩展模块（kb/media/…）对外 `api` 依赖 admin**，下游应用只需要依赖扩展模块即可获得对应能力组合。

### 场景 A：纯净版 Admin (无 KB)
仅需基础权限管理与 AI Key 管理，无需文档解析功能。

```kotlin
// build.gradle.kts
dependencies {
    // 内部源码依赖（推荐开发时使用）
    implementation(project(":flexboot4-admin"))
}
```

### 场景 B：知识库扩展版 (Admin + KB)
需要 RAG 知识库能力，需处理 PDF/Word 文档。

```kotlin
// build.gradle.kts
dependencies {
    // 引入 KB 模块会自动传递引入 Admin 模块
    implementation(project(":flexboot4-kb"))
}
```

### 场景 C：视频中心扩展版 (Admin + Media)
需要媒体扩展能力（例如视频/音频处理、转码、封面抽帧、媒体元数据等）。

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":flexboot4-media"))
}
```

### 场景 D：知识库 + 视频中心 完全版 (Admin + KB + Media)
同时需要知识库与媒体扩展能力。

```kotlin
// build.gradle.kts
dependencies {
    // 引入 KB 模块，会自动传递引入 admin 和 core
    implementation(project(":flexboot4-kb"))
    // 引入 media模块，则会自动穿衣admin 和core
    // admin 会被传递引入 （一般不会重复冲突，Gradle 会做依赖图合并）
    implementation(project(":flexboot4-media"))
}
```

> **提示**：`flexboot4-bootstrap` 是 “知识库扩展版” 的参考组装模块。你也可以按同样方式新增 `flexboot4-bootstrap-media` / `flexboot4-bootstrap-all` 做不同组合的启动入口。

### 如何新增扩展模块（以 Media 为例）
当后续需要扩展项目能力（例如新增 `flexboot4-media`），推荐按以下方式扩展：

1. 新建子工程目录：`flexboot4-media/`
2. 在根工程 [settings.gradle.kts](file:///g:/flexboot4/flexboot4/settings.gradle.kts) 中聚合模块名：

```kotlin
include(":flexboot4-media")
```

3. 在 `flexboot4-media/build.gradle.kts` 中按扩展模块约定配置依赖关系：

```kotlin
dependencies {
    // 扩展模块对外透传 admin 能力：下游只依赖 media 即可拥有 admin + media
    api(project(":flexboot4-admin"))
}
```

## 🔖 版本管理

本项目采用 **Gradle Version Catalog** (`gradle/libs.versions.toml`) 统一管理版本。

### 内部开发（Monorepo）
在当前工程源码内引用时，推荐使用 `project(":xxx")` 语法，此时**无需指定版本号**，直接引用最新源码。

### 外部引用（作为二方库）
如果将 FlexBoot4 发布到 Maven 私服供其他独立项目使用，推荐在外部项目的 `libs.versions.toml` 中配置：

```toml
[versions]
flexboot = "x.x.1-SNAPSHOT"

[libraries]
flexboot-admin = { module = "com.yunlbd:flexboot4-admin", version.ref = "flexboot" }
flexboot-kb = { module = "com.yunlbd:flexboot4-kb", version.ref = "flexboot" }
```

然后外部项目如何引用：
```kotlin
dependencies {
    // 此时必须指定版本号，因为 Gradle 去仓库里找，不知道你要哪个版本
    implementation("com.yunlbd:flexboot4-kb:x.x.1-SNAPSHOT")
}
```

## 🚀 快速开始

### 前置要求
- **JDK 25** (必须)
- **Redis 7+** (建议集群部署)
- **PostgreSQL 16+**(使用kb服务,需额外安装pgVector插件)

### 1. 数据库初始化
执行 `doc/sql/sys_oper_log_pg.sql` 及 `flexboot4-admin/flexboot4-*.sql` 初始化表结构与基础数据。

### 2. 配置环境
修改各模块的 `src/main/resources/application.yml` 配置数据库与 Redis。

### 3. 构建与运行

使用 Gradle Wrapper 编译整个项目：

```bash
./gradlew clean build -x test
```

**启动完整后台 (Admin + KB):**
```bash
# 使用 bootstrap 模块启动
java -jar flexboot4-bootstrap/build/libs/flexboot4-bootstrap-x.x.1-SNAPSHOT.jar
```
访问文档: `http://localhost:8080/scalar`

**启动 AI Gateway:**
```bash
java -jar flexboot4-ai/build/libs/flexboot4-ai-x.x.1-SNAPSHOT.jar
```

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议开源。
