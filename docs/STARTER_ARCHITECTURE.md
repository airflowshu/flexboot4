# Flexboot4 架构重构说明

## 模块结构

重构后的 flexboot4 采用 **Starter 架构**，清晰分离了库模块和应用模块：

### 核心模块

- **flexboot4-core**: 纯 Java 共享库，无 Spring 依赖
- **flexboot4-bom**: Bill of Materials，统一管理所有模块版本

### Starter 模块（可作为库引入）

- **flexboot4-admin-starter**: 提供 RBAC、用户管理、权限控制等基础能力
- **flexboot4-kb-starter**: 提供知识库功能（文档解析、存储等）
- **flexboot4-media-starter**: 提供媒体处理能力（视频、音频等）
- **flexboot4-sms4j-starter**: 提供短信发送与厂商配置管理能力（基于 sms4j）

### 应用模块（可独立运行）

- **flexboot4-bootstrap**: 内部开发测试用，聚合所有 starter

### 其他模块

- **flexboot4-ai**: AI 网关服务（独立模块）

## 使用方式

### 方式一：使用 BOM 统一管理版本（推荐）

在外部项目的 `build.gradle.kts` 中：

```kotlin
plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
}

dependencies {
    // 引入 BOM，统一版本管理
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    // 引入基础 RBAC 能力（必选）
    implementation("com.yunlbd:flexboot4-admin-starter")
    
    // 如果需要知识库功能（可选）
    implementation("com.yunlbd:flexboot4-kb-starter")
    
    // 如果需要媒体处理功能（可选）
    implementation("com.yunlbd:flexboot4-media-starter")

    // 如果需要短信能力（可选）
    implementation("com.yunlbd:flexboot4-sms4j-starter")
}
```

### 方式二：直接指定版本

```kotlin
dependencies {
    // 引入基础 RBAC 能力（必选）
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
    
    // 如果需要知识库功能（可选）
    implementation("com.yunlbd:flexboot4-kb-starter:0.0.1-SNAPSHOT")
    
    // 如果需要媒体处理功能（可选）
    implementation("com.yunlbd:flexboot4-media-starter:0.0.1-SNAPSHOT")

    // 如果需要短信能力（可选）
    implementation("com.yunlbd:flexboot4-sms4j-starter:0.0.1-SNAPSHOT")
}
```

### 方式三：本地开发时使用（Composite Build）

如果在本地同时开发 flexboot4 和外部项目：

在外部项目的 `settings.gradle.kts` 中：

```kotlin
includeBuild("../flexboot4")
```

然后在 `build.gradle.kts` 中：

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-kb-starter")
    implementation("com.yunlbd:flexboot4-media-starter")
    implementation("com.yunlbd:flexboot4-sms4j-starter")
}
```

## 外部项目示例

### 最小化示例（仅使用基础 RBAC）

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
}
```

```java
// YourApplication.java
package com.example.yourapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",  // 扫描 flexboot4 的组件
    "com.example.yourapp"     // 扫描自己的组件
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 完整示例（使用所有功能）

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-kb-starter")
    implementation("com.yunlbd:flexboot4-media-starter")
    implementation("com.yunlbd:flexboot4-sms4j-starter")
}
```

```java
// YourApplication.java
package com.example.yourapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",
    "com.example.yourapp"
})
@EnableCaching
@EnableAsync
@MapperScan({"com.yunlbd.flexboot4.mapper", "com.example.yourapp.mapper"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 配置说明

在外部项目的 `application.yml` 中，需要配置数据库、Redis 等必要信息：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379

# 如果使用了 KB 模块，可能需要额外配置
flexboot4:
  kb:
    storage-path: /path/to/kb/storage

# 如果使用了 Media 模块，可能需要额外配置
flexboot4:
  media:
    storage-path: /path/to/media/storage

# 如果使用了 SMS4J 模块，可能需要额外配置（可通过环境变量覆盖默认值）
sms:
  account-max: ${SMS_ACCOUNT_MAX_TIMES:10}
  minute-max: ${SMS_ACCOUNT_RANGE_TIME:1}
  is-print: false
```

## 发布到 Maven 仓库

当需要将 flexboot4 发布到 Maven 仓库供外部使用时：

```bash
# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal

# 发布到远程 Maven 仓库（需要配置仓库信息）
./gradlew publish
```

## 内部开发

### 运行 Bootstrap（测试所有功能）

```bash
./gradlew :flexboot4-bootstrap:bootRun
```

### 构建所有模块

```bash
./gradlew build
```

## 迁移指南

### 从旧架构迁移

如果之前直接依赖了 `flexboot4-admin`、`flexboot4-kb`、`flexboot4-media`，需要修改为：

**旧方式：**
```kotlin
dependencies {
    implementation(project(":flexboot4-admin"))
    implementation(project(":flexboot4-kb"))
}
```

**新方式：**
```kotlin
dependencies {
    implementation(project(":flexboot4-admin-starter"))
    implementation(project(":flexboot4-kb-starter"))
    implementation(project(":flexboot4-sms4j-starter"))
}
```

## 目录结构对照

```
flexboot4/
├── flexboot4-bom/              # BOM 模块（新增）
├── flexboot4-core/             # 核心库（不变）
├── flexboot4-admin-starter/    # Admin Starter（原 flexboot4-admin 拆分）
├── flexboot4-kb-starter/       # KB Starter（原 flexboot4-kb 重命名）
├── flexboot4-media-starter/    # Media Starter（原 flexboot4-media 重命名）
├── flexboot4-sms4j-starter/    # SMS4J Starter（短信厂商配置与发送能力）
├── flexboot4-bootstrap/        # 内部开发测试（角色调整）
└── flexboot4-ai/               # AI 网关（独立模块）
```

## 优势

1. **清晰的职责划分**：Starter 是库，Bootstrap 是内部测试入口
2. **灵活的模块组合**：按需引入 KB、Media、SMS 等扩展
3. **统一的版本管理**：通过 BOM 避免版本冲突
4. **便于发布**：Starter 可发布到 Maven 仓库供外部使用
5. **降低耦合**：Bootstrap 仅用于内部开发，不对外暴露

## 注意事项

1. **包扫描**：外部项目需要在 `@SpringBootApplication` 中配置 `scanBasePackages`，包含 `com.yunlbd.flexboot4`
2. **Mapper 扫描**：如果使用 MyBatis，需要在 `@MapperScan` 中包含 `com.yunlbd.flexboot4.mapper`
3. **配置文件**：Admin Starter 不包含 `application.yml`，外部项目需自行配置
4. **数据库**：Admin Starter 依赖 PostgreSQL，确保外部项目配置了正确的数据源
