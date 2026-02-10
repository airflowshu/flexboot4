# FlexBoot4 AI Platform

<div align="center">

![FlexBoot4 Logo](https://img.shields.io/badge/FlexBoot-4.0-blue?style=for-the-badge&logo=springboot)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![MyBatis-Flex](https://img.shields.io/badge/MyBatis--Flex-1.11.6-red?style=flat-square)](https://mybatis-flex.com/)
[![Gradle](https://img.shields.io/badge/Gradle-9.x-02303A?style=flat-square&logo=gradle)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-336791?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?style=flat-square&logo=redis)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](./LICENSE)

**新一代企业级 AI 中台与后台管理聚合系统 | Spring Boot 4 + WebFlux + AI Gateway**

[功能特性](#-功能特性) • [技术栈](#-技术栈) • [项目结构](#-项目结构) • [快速开始](#-快速开始) • [文档](#-文档)

</div>

## 📖 简介

**FlexBoot4** 是一个基于 Spring Boot 4 (Java 25) 构建的现代化多模块聚合工程。它创新性地将传统的 **Admin 后台管理** 与 **AI 智能网关** 进行架构分离，通过 Redis Stream 实现跨模块日志审计，旨在为企业提供一套高性能、可扩展的 AI 中台解决方案。

核心理念：**Admin 负责 IAM 与规则定义，AI Gateway 负责高性能运行时执行。**

## ✨ 功能特性

### 🛡️ Admin Server (管理后台)
- **RBAC 权限系统**：基于 Spring Security + JWT，支持方法级权限控制 (`@RequirePermission`)。
- **动态分表审计**：操作日志 (`sys_oper_log`) 自动按季度分表 (`_YYYY_qN`)，支持跨季度查询。
- **数据脱敏**：内置脱敏工具，支持手机、身份证、银行卡等 8 种敏感数据自动脱敏。
- **API Key 管理**：全生命周期管理 AI 访问密钥，支持配额、模型权限与状态控制。
- **MyBatis-Flex 增强**：集成动态表名、多租户、逻辑删除与乐观锁。

### 🤖 AI Gateway (智能网关)
- **高性能 WebFlux**：基于 Reactor 响应式编程，支持高并发 AI 请求转发。
- **API Key 离线鉴权**：基于 Snapshot + Cache 机制，无需 RPC 即可完成鉴权与配额校验。
- **统一日志汇聚**：通过 Redis Stream 将 AI 调用日志（含 Token 消耗、Latency、Prompt Hash）异步投递至 Admin Server 落库。
- **流式响应支持**：原生支持 SSE (Server-Sent Events) 流式数据透传。
- **双模鉴权**：同时支持 `Bearer JWT` (管理端) 与 `Bearer API_KEY` (API 端)。

### 🧩 Common (契约层)
- **纯净依赖**：仅包含 POJO、Enums、Annotations 与工具类，无运行时副作用。
- **统一契约**：定义 `AuthContext`、`OperLogRecord` 等跨模块标准数据结构。

## 🛠 技术栈

| 类别 | 技术框架                     | 说明 |
| --- |--------------------------| --- |
| **Core** | Spring Boot 4.0.2        | 核心框架 (Java 25) |
| **ORM** | MyBatis-Flex 1.11.6      | 灵活的持久层框架 |
| **Database** | PostgreSQL + R2DBC       | 关系型数据库 (Admin: JDBC, AI: R2DBC) |
| **Cache/MQ** | Redis 7.x                | 缓存、限流与 Redis Stream 消息队列 |
| **Auth** | Spring Security + JJWT   | 认证与授权 |
| **Build** | Gradle (Kotlin DSL)      | 构建工具，使用 Version Catalog |
| **Docs** | SpringDoc + Scalar       | API 文档与调试 UI |
| **Tooling** | Lombok, EasyExcel, YAUAA | 开发效率工具 |

## 📂 项目结构

```bash
flexboot4
├── admin-server            # [模块] 后台管理主服务 (Spring MVC)
│   ├── src/main/java
│   │   ├── config          # MyBatis-Flex 动态分表、Redis Stream 消费配置
│   │   ├── controller      # 系统管理与审计日志 API
│   │   ├── listener        # 监听 Redis Stream 消费日志并落库
│   │   └── security        # JWT 签发与 RBAC 权限拦截
│   └── src/main/resources  # logback, application.yml
├── ai-gateway              # [模块] AI 智能网关 (Spring WebFlux)
│   ├── src/main/java
│   │   ├── log             # AOP 日志采集 -> Redis Stream Sink
│   │   ├── security        # 响应式 JWT/API Key 鉴权 Filter
│   │   └── web             # SSE 流式接口与 AI 转发
│   └── src/main/resources  # R2DBC 配置
├── common                  # [模块] 公共契约层 (Pure Java)
│   ├── annotation          # @OperLog, @RequirePermission
│   ├── apikey              # ApiKeySnapshot, ApiKeyRule DTO
│   ├── auth                # AuthContext, JwtScopes
│   └── operlog             # OperLogRecord, OperLogSink 接口
├── gradle                  # Gradle Version Catalog (libs.versions.toml)
└── doc                     # 项目文档与 SQL 脚本
```

## 🚀 快速开始

### 前置要求
- **JDK 25** (必须)
- **Redis 7+**
- **PostgreSQL 16+**

### 1. 数据库初始化
执行 `doc/sql/sys_oper_log_pg.sql` 及 `admin-server/flexboor4-*.sql` 初始化表结构与基础数据。

### 2. 配置环境
修改 `admin-server` 和 `ai-gateway` 的 `src/main/resources/application.yml`，配置你的数据库与 Redis 连接。

**Admin Server (JDBC):**
```yaml
mybatis-flex:
  datasource:
    ds1:
      url: jdbc:postgresql://localhost:5432/flexboot4
      username: postgres
      password: password
```

**AI Gateway (R2DBC):**
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5433/flexboot4_ai
```

### 3. 构建与运行

使用 Gradle Wrapper 编译整个项目：

```bash
./gradlew clean build -x test
```

**启动 Admin Server:**
```bash
java -jar admin-server/build/libs/admin-server-0.0.1-SNAPSHOT.jar
```
访问文档: `http://localhost:8080/scalar`

**启动 AI Gateway:**
```bash
java -jar ai-gateway/build/libs/ai-gateway-0.0.1-SNAPSHOT.jar
```

## 📚 开发指南

### 新增 API 权限控制
在 Controller 方法上添加注解即可：

```java
@RequirePermission("sys:user:add")
@PostMapping
public ApiResult<Void> add(@RequestBody UserDto user) { ... }
```

### 开启操作日志审计
在需要审计的方法上添加 `@OperLog`：

```java
@OperLog(title = "创建API Key", businessType = BusinessType.INSERT)
@PostMapping("/api-key")
public ApiResult<Void> createKey(...) { ... }
```
- **Admin 端**：自动记录操作人、IP、参数并落库。
- **AI 端**：自动记录 Token 消耗、Latency、Prompt Hash 并异步汇聚到 Admin。

## 📄 开源协议

本项目采用 [Apache License 2.0](LICENSE) 协议开源。
