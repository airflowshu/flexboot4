# Flexboot4 常见问题 FAQ

## 目录

- [部署与运行](#部署与运行)
- [登录与认证](#登录与认证)
- [缓存问题](#缓存问题)
- [数据库](#数据库)
- [构建问题](#构建问题)

---

## 部署与运行

### Q1: 如何运行项目？

**A:** 根据需求选择不同的运行方式：

```bash
# 方式一：运行 Bootstrap（完整功能：Admin + KB + Media + SMS4J）
./gradlew :flexboot4-bootstrap:bootRun

# 方式二：运行 AI Gateway（独立服务）
./gradlew :flexboot4-ai:bootRun
```

### Q2: 启动时提示端口被占用怎么办？

**A:** 修改 `application.yml` 中的端口配置：

```yaml
server:
  port: 8081  # 改为其他未占用的端口
```

或者通过命令行参数：

```bash
./gradlew :flexboot4-bootstrap:bootRun --args='--server.port=8081'
```

---

## 登录与认证

### Q3: 登录时返回 401 错误 🔴

**问题现象：**
```
WARN - Authentication failed: Null key returned for cache operation...
```

**原因：** 缺少 `-parameters` 编译参数，导致 Spring Cache 无法解析方法参数名。

**解决方案：** ✅ 已在 v0.0.1-SNAPSHOT 中修复

如果仍然遇到此问题：
1. 确认使用最新代码
2. 重新编译：`./gradlew clean build`
3. 重启应用

认证与权限链路说明见：[权限设计](backend_permission_control_design.md)

### Q4: 默认的登录账号是什么？

**A:** 

| 用户名 | 密码         | 角色 | 说明 |
|--------|------------|------|------|
| `admin` | `admin123` | 管理员 | 完整权限 |
| `super` | `11111111` | 超级管理员 | 系统管理员 |

**首次登录后请立即修改密码！**

### Q5: Token 过期时间是多久？

**A:** 默认 24 小时（86400000 毫秒），可在 `application.yml` 中配置：

```yaml
flexboot4:
  jwt:
    expiration: 86400000  # 24小时，单位：毫秒
```

### Q6: 如何退出登录？

**A:** 调用登出接口：

```bash
POST /api/auth/logout
Authorization: Bearer <your-token>
```

或前端清除本地存储的 token。

### Q6.1: MFA 的密钥应该如何配置？

**A:** MFA 使用标准 TOTP。用户绑定认证器时生成的 TOTP secret 会加密保存到 `sys_user_mfa.secret_ciphertext`，加密密钥来自：

```yaml
flexboot4:
  security:
    mfa-secret-key: ${FLEXBOOT4_SECURITY_MFA_SECRET_KEY}
```

生产环境建议通过环境变量、Docker Secret、Kubernetes Secret 或外部配置中心注入，不要写入仓库：

```bash
export FLEXBOOT4_SECURITY_MFA_SECRET_KEY="$(openssl rand -base64 32)"
```

同一环境的所有后端实例必须使用同一个值，并且跨容器重建保持不变。修改该密钥后，已有 MFA 绑定无法解密，需要用户重新绑定或执行专门迁移。详细说明见 [Admin 认证与账号安全](admin-auth-security.md)。

### Q6.2: 开发环境新增了前端依赖，需要手动 pnpm install 吗？

**A:** 如果当前工作区已经执行过 `pnpm install` 并且 `pnpm-lock.yaml` 已更新，不需要重复执行。其他开发者拉取代码、CI 或新环境首次运行时，应按锁文件安装：

```bash
pnpm install --frozen-lockfile
```

本次 MFA 二维码功能依赖 `@vueuse/integrations` 与 `qrcode`。

---

## 缓存问题

### Q7: Redis 连接失败

**问题现象：**
```
Unable to connect to Redis
```

**解决方案：**

1. **确认 Redis 已启动**
   ```bash
   # Windows
   redis-server.exe
   
   # Linux/Mac
   redis-server
   ```

2. **检查配置**
   ```yaml
   spring:
     redis:
       host: localhost
       port: 6379
       password: # 如果有密码
   ```

3. **测试连接**
   ```bash
   redis-cli ping
   # 应该返回：PONG
   ```

### Q8: 缓存数据不更新

**A:** 可能的原因和解决方案：

1. **缓存未清除**
   - 检查 `@CacheEvict` 注解是否正确
   - 手动清除：`redis-cli FLUSHDB`

2. **缓存配置错误**
   - 检查 `spring.cache` 配置
   - 确认 `@EnableCaching` 已启用

3. **缓存 Key 冲突**
   - 检查不同方法是否使用了相同的 cache name 和 key

---

## 数据库

### Q9: 数据库连接失败

**问题现象：**
```
Unable to connect to database
```

**解决方案：**

1. **确认 PostgreSQL 已启动**
   ```bash
   # 检查服务状态
   systemctl status postgresql  # Linux
   pg_ctl status                # Windows
   ```

2. **检查连接配置**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/flexboot4
       username: postgres
       password: your_password
   ```

3. **创建数据库**
   ```sql
   CREATE DATABASE flexboot4;
   ```

4. **执行初始化脚本**
   ```bash
   psql -U postgres -d flexboot4 -f flexboot4-admin-starter/src/main/resources/db/init.sql
   psql -U postgres -d flexboot4 -f flexboot4-admin-starter/src/main/resources/db/menu_data.sql
   ```

### Q10: 表不存在错误

**A:** 按已集成的 starter 执行数据库初始化脚本。基础后台能力至少需要：

```bash
psql -U postgres -d flexboot4 -f flexboot4-admin-starter/src/main/resources/db/init.sql
psql -U postgres -d flexboot4 -f flexboot4-admin-starter/src/main/resources/db/menu_data.sql
```

如集成 CMS、Media、SMS4J、KB 等能力，再执行对应模块的 `src/main/resources/db/init.sql` 与 `src/main/resources/db/menu_data.sql`。后续功能改动通过各模块 `src/main/resources/db/migration/...` 下的 Flyway 脚本管理。

或者在应用配置中启用自动建表（仅开发环境）：

```yaml
mybatis-flex:
  configuration:
    auto-mapping-behavior: full
```

### Q11: 如何切换数据库（PostgreSQL → MySQL）？

**A:** 

1. **修改依赖**
   ```kotlin
   // build.gradle.kts
   implementation("com.mysql:mysql-connector-j:8.0.33")
   // 移除 PostgreSQL 依赖
   ```

2. **修改配置**
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/flexboot4
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

3. **调整 SQL 脚本**
   - PostgreSQL 和 MySQL 的 SQL 语法有差异
   - 需要修改建表脚本

---

## 构建问题

### Q12: 编译失败 - 找不到符号

**A:** 

1. **清理构建缓存**
   ```bash
   ./gradlew clean
   ```

2. **重新下载依赖**
   ```bash
   ./gradlew build --refresh-dependencies
   ```

3. **检查 Java 版本**
   ```bash
   java -version
   # 应该是 Java 25
   ```

4. **IDE 缓存清理**（IDEA）
   - File → Invalidate Caches → Invalidate and Restart

### Q13: Gradle 下载依赖太慢

**A:** 配置国内镜像：

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        mavenCentral()
    }
}
```

### Q14: 编译时内存不足

**A:** 增加 Gradle 内存：

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

---

## 模块相关

### Q15: 如何只引入 Admin 功能，不要 KB 和 Media？

**A:** 在外部项目中只引入 `admin-starter`：

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
}
```

如需短信能力，可额外引入 `sms4j-starter`：

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
    implementation("com.yunlbd:flexboot4-sms4j-starter:0.0.1-SNAPSHOT")
}
```

### Q16: 如何添加自定义模块？

**A:** 

1. **创建模块目录**
   ```bash
   mkdir flexboot4-custom-starter
   ```

2. **在 settings.gradle.kts 中注册**
   ```kotlin
   include(":flexboot4-custom-starter")
   ```

3. **创建 build.gradle.kts**
   ```kotlin
   dependencies {
       api(project(":flexboot4-admin-starter"))
       // 添加自定义依赖
   }
   ```

4. **编写代码**
   - 创建 Controller、Service、Entity 等

---

## 性能问题

### Q19: 应用启动很慢

**A:** 可能的原因：

1. **首次启动**
   - 需要初始化 Yauaa（User Agent 解析器）
   - 正常现象，约需 1-2 秒

2. **数据库连接慢**
   - 检查网络连接
   - 优化连接池配置

3. **扫描包过多**
   - 缩小 `@ComponentScan` 范围
   - 只扫描必要的包

### Q20: 接口响应慢

**A:** 排查步骤：

1. **查看日志**
   - 是否有慢查询
   - 是否有异常

2. **检查缓存**
   - 缓存是否生效
   - Redis 是否正常

3. **数据库优化**
   - 添加索引
   - 优化 SQL

4. **启用监控**
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,metrics
   ```

---

## 文档相关

### Q21: 如何查看 API 文档？

**A:** 启动应用后访问：

```
http://localhost:8080/scalar/index.html
```

### Q22: 更多文档在哪里？

**A:** 

| 文档 | 说明 |
|------|------|
| [开发者指南](guide.md) | 当前 CRUD、权限、vben 菜单路由与测试约束 |
| [模块文档索引](modules.md) | Admin、CMS、Media、SMS、KB、AI 文档入口 |
| [STARTER_ARCHITECTURE.md](STARTER_ARCHITECTURE.md) | Starter 架构设计 |
| [QUICKSTART.md](QUICKSTART.md) | 快速开始指南 |
| [SMS4J_STARTER.md](../flexboot4-sms4j-starter/SMS4J_STARTER.md) | SMS4J Starter 接入说明 |
| [flexboot4-admin-starter/README.md](../flexboot4-admin-starter/README.md) | Admin Starter 使用说明 |

---

## 获取帮助

### Q23: 遇到问题怎么办？

**A:** 

1. **查看日志**
   ```bash
   tail -f logs/flexboot4.log
   ```

2. **搜索现有 Issue**
   - 检查 GitHub Issues

3. **提问前准备**
   - 完整的错误日志
   - 环境信息（OS、Java 版本、数据库版本）
   - 复现步骤

4. **提交 Issue**
   - 使用 Issue 模板
   - 提供足够的上下文

---

**最后更新**：2026-02-26  
**版本**：v0.0.1-SNAPSHOT

如有其他问题，欢迎提交 Issue 或 PR！
