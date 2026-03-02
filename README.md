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

**新一代企业级 AI 中台与后台管理聚合系统 | Spring Boot 4 + SpringMVC/WebFlux + AI Gateway**

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
├── flexboot4-bom               # [BOM] 统一版本管理
├── flexboot4-core              # [基座] 基础设施 (Pure Java)
│   └── DTO, Enums, Utils, 统一契约
├── flexboot4-admin-starter     # [Starter] Admin 功能库 (RBAC, Audit, System)
│   └── 纯库模块，供外部项目引入
├── flexboot4-kb-starter        # [Starter] 知识库扩展 (PDF/Word 解析, 向量化)
│   └── 依赖 admin-starter，提供 RAG 能力
├── flexboot4-media-starter     # [Starter] 媒体扩展 (视频/音频处理)
│   └── 依赖 admin-starter，提供媒体能力
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
   - `flexboot4-admin-starter`: 提供 RBAC、用户管理、权限控制等基础能力
   - `flexboot4-kb-starter`: 提供知识库功能
   - `flexboot4-media-starter`: 提供媒体处理能力

2. **内部开发模块**
   - `flexboot4-bootstrap`: 内部开发测试应用（聚合所有 starter）

3. **独立服务**
   - `flexboot4-ai`: AI 网关服务（独立部署）

### 依赖关系

```
flexboot4-core (纯 Java 库)
    ↑
flexboot4-admin-starter (基础 RBAC)
    ↑
├── flexboot4-kb-starter (知识库扩展)
└── flexboot4-media-starter (媒体扩展)
```

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
    // 注意：kb-starter 使用 api 依赖了 admin-starter，会自动传递引入
    // ✅ admin-starter 会自动传递，无需显式引入 admin-starter
    implementation("com.yunlbd:flexboot4-kb-starter")
}
```

### 场景 C：外部项目引入Admin + Media
在 `build.gradle.kts` 中：

```kotlin
implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
implementation("com.yunlbd:flexboot4-media-starter")
// ✅ admin-starter 会自动传递，无需显式引入

```

### 场景 D：完整功能（Admin + KB + Media）
在 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    // 基础 RBAC 能力（可选显式引入，也可通过 kb/media 自动传递）
    implementation("com.yunlbd:flexboot4-admin-starter")

    // 知识库能力（会自动传递 admin-starter）
    implementation("com.yunlbd:flexboot4-kb-starter")

    // 媒体处理能力（会自动传递 admin-starter）
    implementation("com.yunlbd:flexboot4-media-starter")
}
```
> **💡 提示**：
> - `kb-starter` 和 `media-starter` 都使用 `api` 依赖了 `admin-starter`
> - 理论上只需引入 kb/media，admin 会自动传递
> - **但推荐显式引入 admin-starter**，使依赖关系更清晰


### 场景 E：内部开发测试

```bash
# 运行 Bootstrap（所有功能）
./gradlew :flexboot4-bootstrap:bootRun
```

详细使用说明请参考 [Starter 架构文档](doc/STARTER_ARCHITECTURE.md)

## 📝 外部项目集成示例

创建主应用类：

```java
package com.example.yourapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",  // 扫描 flexboot4 组件
    "com.example.yourapp"     // 扫描自己的组件
})
@EnableCaching
@MapperScan({"com.yunlbd.flexboot4.mapper", "com.example.yourapp.mapper"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 🔖 版本管理

本项目采用 **Gradle Version Catalog** (`gradle/libs.versions.toml`) 统一管理内部依赖版本。

### 内部开发（Monorepo）

在当前工程源码内引用时，推荐使用 `project(":xxx")` 语法，此时**无需指定版本号**，直接引用最新源码：

```kotlin
// 内部模块间依赖（在 flexboot4 项目内）
dependencies {
    api(project(":flexboot4-admin-starter"))
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
- `com.yunlbd:flexboot4-admin-starter` - Admin Starter
- `com.yunlbd:flexboot4-kb-starter` - KB Starter
- `com.yunlbd:flexboot4-media-starter` - Media Starter

> **注意**：`flexboot4-bootstrap` 作为内部开发测试模块，**不会发布到 Maven 仓库**。

## 🚀 快速开始

### 前置要求
- **JDK 25** (必须)
- **Redis 7+** (建议集群部署)
- **PostgreSQL 16+**（使用 KB 服务需额外安装 pgVector 插件）

### 1. 数据库初始化
执行 `doc/sql/sys_oper_log_pg.sql` 及相关 SQL 脚本初始化表结构与基础数据。

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
包含：Admin + KB + Media 所有功能

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

#### 详细文档

- [🌟 优雅的自动加载方案](./doc/ELEGANT_AUTO_CONFIGURATION.md) - 了解实现原理
- [EXTERNAL_PROJECT_CHECKLIST.txt](./EXTERNAL_PROJECT_CHECKLIST.txt) - 快速参考清单
- [外部项目使用指南](./EXTERNAL_PROJECT_USAGE.md) - 完整说明
- [Starter 架构文档](doc/STARTER_ARCHITECTURE.md)
- [快速开始指南](doc/QUICKSTART.md)
- [Admin Starter 使用说明](./flexboot4-admin-starter/README.md)

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议开源.
