# Flexboot4 快速开始指南

## 目录

- [内部开发](#内部开发)
- [外部项目集成](#外部项目集成)
- [发布到 Maven](#发布到-maven)

---

## 内部开发

### 1. 运行 Bootstrap（所有功能）

```bash
./gradlew :flexboot4-bootstrap:bootRun
```

包含：Admin + KB + Media 所有功能

### 2. 运行 AI Gateway

```bash
./gradlew :flexboot4-ai:bootRun
```

访问：http://localhost:9090

### 3. 构建所有模块

```bash
./gradlew clean build -x test
```

### 4. 发布到本地 Maven 仓库

```bash
./gradlew publishToMavenLocal
```

发布后可在其他项目中使用：
```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
}
```

---

## 外部项目集成

### 方式一：Maven 依赖（推荐生产环境）

#### 1. 发布 Flexboot4 到本地 Maven

在 flexboot4 项目根目录：

```bash
./gradlew publishToMavenLocal
```

#### 2. 在外部项目中引入

`build.gradle.kts`:

```kotlin
plugins {
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
}

repositories {
    mavenLocal()  // 使用本地 Maven 仓库
    mavenCentral()
}

dependencies {
    // 使用 BOM 统一版本
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))
    
    // 引入所需的 starter
    implementation("com.yunlbd:flexboot4-admin-starter")
    // implementation("com.yunlbd:flexboot4-kb-starter")     // 如需知识库
    // implementation("com.yunlbd:flexboot4-media-starter")  // 如需媒体处理
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

#### 3. 创建主应用类

`src/main/java/com/example/yourapp/YourApplication.java`:

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

#### 4. 配置数据库和 Redis

`src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: your-app
  
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  redis:
    host: localhost
    port: 6379
  
  security:
    user:
      name: admin
      password: admin123

# MyBatis-Flex 配置
mybatis-flex:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  mapper-locations: classpath*:/mapper/**/*.xml

# JWT 配置
flexboot4:
  jwt:
    secret: your-secret-key-at-least-256-bits-long-please-change-this
    expiration: 86400000  # 24小时

server:
  port: 8080
```

#### 5. 运行应用

```bash
./gradlew bootRun
```

### 方式二：Composite Build（推荐开发环境）

#### 1. 项目结构

```
workspace/
├── flexboot4/          # Flexboot4 源码
└── your-project/       # 你的项目
```

#### 2. 配置 Composite Build

在你的项目的 `settings.gradle.kts`:

```kotlin
rootProject.name = "your-project"

// 引入 flexboot4 作为复合构建
includeBuild("../flexboot4")
```

#### 3. 引入依赖

`build.gradle.kts`:

```kotlin
dependencies {
    // 直接使用 Maven 坐标，Gradle 会自动从 flexboot4 项目构建
    implementation("com.yunlbd:flexboot4-admin-starter")
    implementation("com.yunlbd:flexboot4-kb-starter")
}
```

#### 4. 开发体验

- **自动重新构建**：修改 flexboot4 代码后，your-project 会自动重新编译
- **无需发布**：不需要 `publishToMavenLocal`
- **快速迭代**：适合同时开发 flexboot4 和业务项目

---

## 发布到 Maven

### 1. 本地 Maven 仓库

```bash
./gradlew publishToMavenLocal
```

发布的模块：
- `com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT`
- `com.yunlbd:flexboot4-core:0.0.1-SNAPSHOT`
- `com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT`
- `com.yunlbd:flexboot4-kb-starter:0.0.1-SNAPSHOT`
- `com.yunlbd:flexboot4-media-starter:0.0.1-SNAPSHOT`

### 2. 远程 Maven 仓库

需要在 `build.gradle.kts` 或 `gradle.properties` 中配置仓库信息：

```kotlin
// 根目录的 build.gradle.kts
allprojects {
    group = "com.yunlbd"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "MyMavenRepo"
                    url = uri("https://maven.example.com/repository")
                    credentials {
                        username = project.findProperty("mavenUser") as String?
                        password = project.findProperty("mavenPassword") as String?
                    }
                }
            }
        }
    }
}
```

然后发布：

```bash
./gradlew publish
```

---

## 常见问题

### Q1: 编译报错找不到 flexboot4 的类

**原因**：未正确配置包扫描。

**解决**：确保 `@SpringBootApplication` 包含 `scanBasePackages`:

```java
@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",
    "com.example.yourapp"
})
```

### Q2: Mapper 找不到

**原因**：未扫描 flexboot4 的 Mapper。

**解决**：在 `@MapperScan` 中包含 flexboot4 的包：

```java
@MapperScan({"com.yunlbd.flexboot4.mapper", "com.example.yourapp.mapper"})
```

### Q3: 数据库连接失败

**原因**：未配置数据库或数据库未启动。

**解决**：
1. 确保 PostgreSQL 已启动
2. 检查 `application.yml` 中的数据库配置
3. 执行初始化 SQL（`doc/sql/` 目录）

### Q4: Redis 连接失败

**原因**：未配置 Redis 或 Redis 未启动。

**解决**：
1. 确保 Redis 已启动
2. 检查 `application.yml` 中的 Redis 配置

### Q5: JWT 认证失败

**原因**：未配置 JWT Secret 或 Secret 不符合要求。

**解决**：在 `application.yml` 中配置至少 256 位的 Secret：

```yaml
flexboot4:
  jwt:
    secret: your-very-long-secret-key-at-least-256-bits
    expiration: 86400000
```

### Q6: Composite Build 时模块找不到

**原因**：`settings.gradle.kts` 配置错误。

**解决**：确保路径正确：

```kotlin
includeBuild("../flexboot4")  // 相对路径要正确
```

---

## 下一步

- [查看完整架构文档](STARTER_ARCHITECTURE.md)
- [Admin Starter 文档](../flexboot4-admin-starter/README.md)

---

## 联系方式

如有问题，请提交 Issue 或联系维护团队。
