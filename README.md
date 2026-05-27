# FlexBoot4 AI Platform

<div align="center">

![FlexBoot4 Logo](https://img.shields.io/badge/FlexBoot-4.0-blue?style=for-the-badge&logo=springboot)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![MyBatis-Flex](https://img.shields.io/badge/MyBatis--Flex-1.11.6-red?style=flat-square)](https://mybatis-flex.com/)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.1-02303A?style=flat-square&logo=gradle)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](./LICENSE)

**新一代企业级 AI 中台与后台管理聚合系统 | Spring Boot 4 + SpringMVC/WebFlux + AI Gateway**

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [项目结构](#-项目结构) • [模块组装](#-模块组装) • [版本管理](#-版本管理) • [快速开始](#-快速开始)

</div>

## 📖 简介

**FlexBoot4** 是一个基于 Spring Boot 4 (Java 25) 构建的现代化多模块聚合工程。它创新性地将传统的 **Admin 后台管理** 与 **AI 智能网关** 进行架构分离，并通过模块化设计支持 **知识库 (RAG)** 能力的按需挂载。

核心理念：**Admin 负责 IAM 与规则定义，KB 负责知识资产沉淀，AI Gateway 负责高性能运行时执行。**

## ✨ 功能特性

### 🛡️ FlexBoot4 Admin (管理内核)
- **RBAC 权限系统**：基于 Spring Security + JWT，支持方法级权限控制 (`@RequirePermission`)。
- **默认拒绝策略**：`/api/admin/**` 下未声明权限且未配置跳过的接口默认拒绝，减少误暴露面。
- **分布式任务锁**：关键 `@Scheduled` 任务通过 `DistributedLockService` 加锁，Redis 可用时自动使用 Redis 锁，避免多实例重复执行。
- **SQL 演进治理**：Admin starter 内置 Flyway 迁移脚本目录，外部项目可按需启用，逐步替代手工执行 SQL 补丁。
- **架构约束固化**：测试中约束生产代码禁止字段注入、禁止注入具体 `*Impl`、约束 starter 依赖边界，防止解耦成果回退。
- **轻量可观测性**：通过 `MetricsRecorder` 统一记录认证失败、权限拒绝、操作日志消费/reclaim、分布式锁抢占等关键链路指标；默认 Noop，可由业务项目替换为 Micrometer/Prometheus 实现。
- **动态分表审计**：操作日志 (`sys_oper_log`) 自动按季度分表 (`_YYYY_qN`)，支持跨季度查询；Redis Stream 消费使用 `event_id` 做数据库幂等，落库成功后再 ack，并支持 pending reclaim 与 dead-letter stream。
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
| **Core** | Spring Boot 4.0.3      | 核心框架 (Java 25) |
| **ORM** | MyBatis-Flex 1.11.6    | 灵活的持久层框架 |
| **Database** | PostgreSQL + R2DBC     | 关系型数据库 (Admin: JDBC, AI: R2DBC) |
| **Cache/MQ** | Redis 7.x              | 缓存、限流与 Redis Stream 消息队列 |
| **Auth** | Spring Security + JJWT | 认证与授权 |
| **Build** | Gradle (Kotlin DSL)    | 构建工具，使用 Version Catalog |
| **Docs** | SpringDoc + Scalar     | API 文档与调试 UI |

## 📂 项目结构

```bash
flexboot4
├── flexboot4-bom               # [BOM] 统一版本管理
├── flexboot4-core              # [基座] 基础设施 (Pure Java)
│   └── DTO, Enums, Utils, 统一契约
├── flexboot4-admin-kernel      # [Kernel] starter 公共底座
│   └── 安全上下文、通用模型、公共配置与基础工具
├── flexboot4-admin-starter     # [Starter] Admin 功能库 (RBAC, Audit, System)
│   └── 基于 admin-kernel 提供 RBAC、系统管理、运维能力
├── flexboot4-kb-starter        # [Starter] 知识库扩展 (PDF/Word 解析, 向量化)
│   └── 依赖 admin-kernel，提供 RAG 能力
├── flexboot4-media-starter     # [Starter] 媒体扩展 (视频/音频处理)
│   └── 依赖 admin-kernel，提供媒体能力
├── flexboot4-sms4j-starter     # [Starter] 短信扩展
│   └── 依赖 admin-kernel，提供多通道短信能力
├── flexboot4-cms-starter       # [Starter] CMS 扩展
│   └── 依赖 admin-kernel，提供模板管理与静态发布
├── flexboot4-bootstrap         # [Internal] 内部开发测试
│   └── 聚合所有 starter，用于内部开发维护
├── flexboot4-ai                # [独立] AI 智能网关 (Spring WebFlux)
│   └── 独立部署，负责 AI 流量代理与计费
└── gradle                      # Gradle Version Catalog
```

## 🏗️ 架构设计

FlexBoot4 采用 **Starter 模式** 设计，将功能模块封装为可重用的库（Starter），外部项目按需引入：

### 模块分类

1. **Starter 模块**（可作为库引入）
   - `flexboot4-admin-kernel`: starter 公共底座，承载跨模块共享的上下文、公共模型与基础配置
   - `flexboot4-admin-starter`: 提供 RBAC、用户管理、权限控制等基础能力
   - `flexboot4-kb-starter`: 提供知识库功能
   - `flexboot4-media-starter`: 提供媒体处理能力
   - `flexboot4-sms4j-starter`: 提供短信发送与渠道配置能力
   - `flexboot4-cms-starter`: 提供 CMS 模板管理与静态发布能力

2. **内部开发模块**
   - `flexboot4-bootstrap`: 内部开发测试应用（聚合所有 starter）

3. **独立服务**
   - `flexboot4-ai`: AI 网关服务（独立部署）

### 依赖关系

```
flexboot4-core (纯 Java 库)
    ↑
flexboot4-admin-kernel (公共底座)
    ↑
├── flexboot4-admin-starter (RBAC / System / OperLog)
├── flexboot4-kb-starter (知识库扩展)
├── flexboot4-media-starter (媒体扩展)
├── flexboot4-sms4j-starter (短信扩展)
└── flexboot4-cms-starter (CMS 扩展)
```

各业务 starter 已解耦到 `admin-kernel`，可只引入公共底座能力；如果需要后台 RBAC、菜单、权限码、文件管理或管理页面，再显式引入 `admin-starter` 或提供等价的文件、配置、用户上下文 Bean。

## 📦 模块组装

### 场景 A：外部项目引入基础 RBAC 能力

在外部项目的 `build.gradle.kts` 中：

```kotlin
dependencies {
    // 使用 BOM 统一版本管理（推荐）
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    // 引入基础 RBAC 能力
    implementation("com.yunlbd:flexboot4-admin-starter")
}
```

### 场景 B：外部项目引入知识库能力

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    // 引入知识库 starter
    implementation("com.yunlbd:flexboot4-kb-starter")

    // 如需后台管理页面、RBAC、文件管理与用户上下文，再显式引入
    // implementation("com.yunlbd:flexboot4-admin-starter")
}
```

### 场景 C：外部项目引入 Media
在 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    // media-starter 只依赖 admin-kernel，不携带 admin-starter
    implementation("com.yunlbd:flexboot4-media-starter")

    // 如需后台管理页面、RBAC 菜单和权限控制，再显式引入
    // implementation("com.yunlbd:flexboot4-admin-starter")
}
```

### 场景 D：完整功能（Admin + KB + Media + SMS + CMS）
在 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    // 基础 RBAC 能力
    implementation("com.yunlbd:flexboot4-admin-starter")

    // 知识库能力（只依赖 admin-kernel）
    implementation("com.yunlbd:flexboot4-kb-starter")

    // 媒体处理能力（只依赖 admin-kernel）
    implementation("com.yunlbd:flexboot4-media-starter")

    // 短信能力（只依赖 admin-kernel）
    implementation("com.yunlbd:flexboot4-sms4j-starter")

    // CMS 能力（只依赖 admin-kernel）
    implementation("com.yunlbd:flexboot4-cms-starter")
}
```
> **💡 提示**：
> - `kb-starter`、`media-starter`、`sms4j-starter`、`cms-starter` 均依赖 `admin-kernel`
> - `kb-starter` 与 `cms-starter` 运行到文件管理、配置读取、用户上下文链路时，需要 `admin-starter` 提供默认实现，或由业务项目提供等价 Bean
> - 需要后台管理能力时，推荐显式引入 `admin-starter`，使依赖关系更清晰


### 场景 E：内部开发测试

```bash
# 运行 Bootstrap（所有功能）
./gradlew :flexboot4-bootstrap:bootRun
```

详细使用说明请参考 [Starter 架构文档](docs/STARTER_ARCHITECTURE.md)

## 📝 外部项目集成示例

创建主应用类：

```java
package com.example.yourapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@MapperScan("com.example.yourapp.mapper")
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

各 starter 已提供 Spring Boot 4 标准 `@AutoConfiguration` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 入口，外部应用不需要再配置 `scanBasePackages("com.yunlbd.flexboot4")`。如果业务侧有自己的 Mapper，只扫描业务包即可。

## 🔖 版本管理

本项目采用 **Gradle Version Catalog** (`gradle/libs.versions.toml`) 统一管理内部依赖版本。

### 内部开发（Monorepo）

在当前工程源码内引用时，推荐使用 `project(":xxx")` 语法，此时**无需指定版本号**，直接引用最新源码：

```kotlin
// 内部模块间依赖（在 flexboot4 项目内）
dependencies {
    api(project(":flexboot4-admin-kernel"))
    implementation(project(":flexboot4-admin-starter"))
    implementation(project(":flexboot4-kb-starter"))
}
```

### 外部引用（作为二方库）

当 FlexBoot4 发布到 Maven 仓库后，外部项目有以下三种引用方式：

#### 方式一：使用 BOM 统一版本管理（推荐）

```kotlin
// 外部项目的 build.gradle.kts
dependencies {
    // 引入 BOM，统一管理所有 flexboot4 模块版本
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    // 无需指定版本，由 BOM 统一管理
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-kb-starter")
    implementation("com.yunlbd:flexboot4-media-starter")
    implementation("com.yunlbd:flexboot4-sms4j-starter")
    implementation("com.yunlbd:flexboot4-cms-starter")
}
```

#### 方式二：直接指定版本

```kotlin
// 外部项目的 build.gradle.kts
dependencies {
    // 每个模块单独指定版本
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
    implementation("com.yunlbd:flexboot4-kb-starter:0.0.1-SNAPSHOT")
    implementation("com.yunlbd:flexboot4-media-starter:0.0.1-SNAPSHOT")
    implementation("com.yunlbd:flexboot4-sms4j-starter:0.0.1-SNAPSHOT")
    implementation("com.yunlbd:flexboot4-cms-starter:0.0.1-SNAPSHOT")
}
```

#### 方式三：Composite Build（本地开发）

如果在本地同时开发 flexboot4 和外部项目：

```kotlin
// 外部项目的 settings.gradle.kts
includeBuild("../flexboot4")

// 外部项目的 build.gradle.kts
dependencies {
    // 使用 Maven 坐标，Gradle 会自动从复合构建中解析
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-kb-starter")
    implementation("com.yunlbd:flexboot4-media-starter")
}
```

### 发布到 Maven 仓库

```bash
# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal

# 发布到远程 Maven 仓库（需要配置仓库信息）
./gradlew publish
```

发布的模块包括：
- `com.yunlbd:flexboot4-bom` - 版本管理 BOM
- `com.yunlbd:flexboot4-core` - 核心库
- `com.yunlbd:flexboot4-admin-kernel` - Starter 公共底座
- `com.yunlbd:flexboot4-admin-starter` - Admin Starter
- `com.yunlbd:flexboot4-kb-starter` - KB Starter
- `com.yunlbd:flexboot4-media-starter` - Media Starter
- `com.yunlbd:flexboot4-sms4j-starter` - SMS4J Starter
- `com.yunlbd:flexboot4-cms-starter` - CMS Starter

> **注意**：`flexboot4-bootstrap` 作为内部开发测试模块，**不会发布到 Maven 仓库**。

## 🚀 快速开始

### 前置要求
- **JDK 25** (必须)
- **Redis 7+** (建议集群部署)
- **PostgreSQL 16+**（使用 KB 服务需额外安装 pgVector 插件）

### 1. 数据库初始化
执行 `docs/sql/sys_oper_log_pg.sql`、`docs/sql/admin_permission_p0_patch_pg.sql` 及相关 SQL 脚本初始化表结构、基础数据与 P0 权限码。已有环境升级 P2 操作日志可靠性时，可执行 `docs/sql/admin_operlog_p2_reliability_pg.sql` 补充 `event_id` 幂等字段与唯一索引。

### 2. 配置环境
修改 `application.yml` 配置数据库与 Redis 连接信息。

### 3. 构建与运行

**构建整个项目：**
```bash
./gradlew clean build -x test
```


**启动 Bootstrap（完整功能）：**
```bash
./gradlew :flexboot4-bootstrap:bootRun
```
包含：Admin + KB + Media + SMS4J + CMS，用于内部集成验证。Bootstrap 会显式启用部分集成特性，例如 `media.enabled=true`、`media.runtime-check-enabled=true`、`file.embedding.stream.enabled=true`。

**启动 AI Gateway：**
```bash
./gradlew :flexboot4-ai:bootRun
```
访问: `http://localhost:9090`

### 4. 外部项目集成

#### 快速开始（两步，无需额外配置！）

**第1步**：在 `build.gradle.kts` 中添加依赖

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:1.0.1")
    // 可选
    // implementation("com.yunlbd:flexboot4-kb-starter:1.0.1")
    // implementation("com.yunlbd:flexboot4-media-starter:1.0.1")
    // implementation("com.yunlbd:flexboot4-sms4j-starter:1.0.1")
    // implementation("com.yunlbd:flexboot4-cms-starter:1.0.1")
}
```

**第2步**：在 `application.yml` 中定义项目配置

```yaml
spring:
  application:
    name: my-awesome-app
  
  data:
    redis:
      host: localhost
      port: 6379

mybatis-flex:
  datasource:
    ds1:
      url: jdbc:postgresql://localhost:5432/mydb
      username: user
      password: pass
```

**完成！** 框架的所有默认配置会**自动加载**：
- ✅ Jackson 时区、日期格式
- ✅ Security 忽略路径
- ✅ OperLog、SpringDoc、Scalar 配置
- ✅ 完全支持覆盖

Media 与 KB 的运行时任务默认保持关闭，避免外部项目引入 starter 后立即启动外部依赖相关任务；需要时显式开启：

```yaml
media:
  enabled: true
  runtime-check-enabled: true

file:
  embedding:
    stream:
      enabled: true

flexboot4:
  flyway:
    admin-migrations-enabled: true
  lock:
    key-prefix: flexboot4:lock:
    default-ttl-millis: 60000

operlog:
  stream:
    enabled: true
    dead-letter-key: operlog:stream:dead
```

启用 `flexboot4.flyway.admin-migrations-enabled=true` 后，Admin starter 会把 `classpath:db/migration/flexboot4/admin/postgresql` 追加到 `spring.flyway.locations`。业务应用仍需自行引入 Flyway 依赖并决定 `spring.flyway.enabled`、baseline 等策略；starter 默认不强制开启迁移，避免影响已有数据库启动流程。

`operlog.stream.dead-letter-key` 默认值为 `operlog:stream:dead`。当消息超过 `max-delivery-attempts`，或 payload 无法解析时，Admin 会把原始消息及失败原因写入死信流，写入成功后再 ack 原消息，便于人工排查和后续重放。

#### 详细文档

- [Starter 架构文档](docs/STARTER_ARCHITECTURE.md)
- [快速开始指南](docs/QUICKSTART.md)
- [Admin Starter 使用说明](./flexboot4-admin-starter/README.md)
- [Media Starter 使用说明](./flexboot4-media-starter/README.md)
- [SMS4J Starter 使用说明](./flexboot4-sms4j-starter/SMS4J_STARTER.md)
- [CMS Starter 使用说明](./flexboot4-cms-starter/README.md)

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议开源.

## CMS 模板管理与静态发布（v1）

`flexboot4-cms-starter` 已补充模板文件管理与静态发布能力，默认模板根目录为 `webapp/html/web`。

### 能力说明

- 递归扫描并管理全部 `.html` 模板文件
- 后台页面直接编辑源码并实时 iframe 预览
- 保存时直接回写模板文件系统
- 发布时原样复制当前模板目录并生成 ZIP 包
- 记录发布批次、目录、ZIP、状态、操作者与错误信息

### 关键配置

```yaml
cms:
  template:
    root-dir: ${CMS_TEMPLATE_ROOT_DIR:${user.dir}/webapp/html/web}
    asset-base-url: ${CMS_TEMPLATE_ASSET_BASE_URL:http://localhost:8080}
    publish-dir: ${CMS_TEMPLATE_PUBLISH_DIR:${cms.render.output-dir}/site-published}
```

### 管理入口

- 后端接口前缀：`/api/admin/cms/template`
- 前端页面：`/cms/template/index`
- 菜单权限：`cms:template:view`、`cms:template:edit`、`cms:template:publish`

### SQL

- 表结构：`docs/sql/cms_pg.sql`
- 菜单：`docs/sql/cms_menu_init_pg.sql`
