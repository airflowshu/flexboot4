# Flexboot4 Admin Starter

Admin Starter 提供了 flexboot4 的核心 RBAC（基于角色的访问控制）功能，包括：

## 功能特性

- **用户管理**：用户注册、登录、权限管理
- **角色管理**：角色定义、权限分配
- **菜单管理**：动态菜单、权限控制
- **部门管理**：组织架构管理
- **操作日志**：系统操作审计，Redis Stream 消费支持 `event_id` 幂等、成功后 ack、pending reclaim 与 dead-letter stream
- **分布式任务锁**：关键定时任务通过 `DistributedLockService` 加锁，Redis 可用时自动使用 Redis 锁
- **Flyway 迁移脚本**：内置 Admin PostgreSQL 迁移目录，可按需追加到业务应用的 Flyway locations
- **架构约束测试**：固化禁止字段注入、禁止注入具体 `*Impl`、starter 边界依赖等规则
- **轻量指标接口**：通过 `MetricsRecorder` 记录认证、权限、OperLog Stream、分布式锁等关键事件，默认 Noop，支持业务项目替换实现
- **登录日志**：用户登录追踪
- **安全认证**：JWT Token 认证，`/api/admin/**` 未声明权限的接口默认拒绝
- **账号安全**：个人中心支持密保手机、备用邮箱和标准 TOTP MFA 设备绑定
- **Redis 缓存**：高性能缓存支持
- **API 文档**：集成 SpringDoc（Scalar UI）

## 依赖

- Spring Boot 4.x
- Spring Security
- MyBatis-Flex
- PostgreSQL
- Redis
- JWT

## 使用方式

### 引入依赖

```kotlin
dependencies {
    implementation("com.yunlbd:flexboot4-admin-starter:0.0.1-SNAPSHOT")
}
```

### 创建主应用类

```java
package com.example.yourapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

Admin Starter 使用 Spring Boot 标准自动装配入口加载框架 Bean，业务应用无需再配置
`scanBasePackages("com.yunlbd.flexboot4")`。如果业务应用有自己的 Mapper，请只扫描业务包：

```java
import org.mybatis.spring.annotation.MapperScan;

@MapperScan("com.example.yourapp.mapper")
```

### 配置 application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flexboot4
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  redis:
    host: localhost
    port: 6379
    password: # 如果有密码
  
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
  flyway:
    admin-migrations-enabled: false
  lock:
    key-prefix: flexboot4:lock:
    default-ttl-millis: 60000
  jwt:
    secret: your-secret-key-at-least-256-bits-long
    expiration: 86400000 # 24小时
  security:
    mfa-secret-key: ${FLEXBOOT4_SECURITY_MFA_SECRET_KEY}

operlog:
  stream:
    enabled: true
    key: operlog:stream
    dead-letter-key: operlog:stream:dead
    max-delivery-attempts: 10
```

`operlog.stream.dead-letter-key` 默认值为 `operlog:stream:dead`。超过最大投递次数的 pending 消息，或 payload 无法解析的消息，会写入死信流并在写入成功后 ack 原消息，便于人工排查与重放。

`flexboot4.security.mfa-secret-key` 用于加密保存用户 TOTP MFA secret。生产环境必须通过环境变量、Secret 管理或外部配置注入稳定随机值，不要提交到仓库。同一环境多实例必须保持一致；修改该密钥会导致已绑定 MFA 的 secret 无法解密。更多说明见 [Admin 认证与账号安全](../docs/admin-auth-security.md)。

## 核心 API

引入 Admin Starter 后，你可以使用以下服务：

```java
@Service
public class YourService {
    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final SysDeptService deptService;

    public YourService(SysUserService userService,
                       SysRoleService roleService,
                       SysMenuService menuService,
                       SysDeptService deptService) {
        this.userService = userService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.deptService = deptService;
    }
}
```

如果业务应用已引入 Flyway，并希望自动纳入 Admin starter 的 SQL 演进脚本，可设置：

```yaml
flexboot4:
  flyway:
    admin-migrations-enabled: true
```

启用后会追加迁移目录 `classpath:db/migration/flexboot4/admin/postgresql`。starter 默认不强制启用 Flyway，避免影响已有项目的数据库启动策略。

## SQL

- 首次接入初始化：`src/main/resources/db/init.sql`
- 菜单与按钮权限：`src/main/resources/db/menu_data.sql`
- 后续演进迁移：`src/main/resources/db/migration/flexboot4/admin/postgresql`

### 可观测性扩展

默认提供 Noop `MetricsRecorder`，不会引入额外监控依赖。业务项目可声明自己的 Bean 接入 Micrometer、Prometheus 或内部监控：

```java
@Bean
MetricsRecorder metricsRecorder() {
    return new YourMetricsRecorder();
}
```

当前内置打点覆盖登录成功/失败/锁定、JWT 无效、权限拒绝、OperLog Stream 持久化/重复/reclaim、Redis 分布式锁抢占/释放。

## 注解使用

### 权限控制

```java
@RestController
@RequestMapping("/api/admin/example-users")
public class UserController {
    
    @RequirePermission("sys:user:list")
    @GetMapping("/{id}")
    public ApiResult<SysUserDetailVO> get(@PathVariable String id) {
        // ...
    }
    
    @RequirePermission("sys:user:add")
    @PostMapping
    public ApiResult<Boolean> add(@RequestBody SysUserCreateReq request) {
        // ...
    }
}
```

P0 安全收口后，Admin 接口应显式声明 `@RequirePermission`；`/api/admin/**` 下未声明权限且未配置跳过的接口会被默认拒绝。当前关键权限码包括：

- `sys:user:reset-password`
- `sys:oper:log:list`
- `sys:monitor:stats`

登录、找回密码、重置密码、静态资源、OpenAPI/Scalar 与 `/error` 保持最小白名单。

### 数据权限

数据权限仍处于设计规划阶段，当前主线只启用接口级 RBAC。相关草案见
`../docs/TODO 实现基于注解和 MyBatis 拦截器的数据权限控制.md`。

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 扩展开发

如果需要扩展 Admin Starter 的功能，可以：

1. **添加自定义 Controller**：在自己的包下创建 Controller
2. **新增业务资源**：按 DTO/VO/Mapper/Controller 契约新增业务模块
3. **自定义认证逻辑**：实现 `UserDetailsService` 接口

## 注意事项

1. Admin Starter 是纯库模块，通过默认配置文件提供框架级默认值，外部项目仍需提供数据库、Redis、JWT 等环境配置
2. 外部项目需要配置数据库连接、Redis 连接等信息
3. 确保数据库已创建相应的表结构与权限数据：首次接入执行本模块 `src/main/resources/db/init.sql` 和 `src/main/resources/db/menu_data.sql`；后续改动通过本模块 Flyway 迁移目录维护
4. JWT Secret 建议使用至少 256 位的随机字符串
5. MFA Secret 建议使用独立于 JWT Secret 的稳定随机字符串，生产环境通过 `FLEXBOOT4_SECURITY_MFA_SECRET_KEY` 注入
6. `flexboot4-admin-kernel` 仅承载公共底座类，不会自动启用 RBAC/运维 Bean；kb/media/sms/cms starter 已按 kernel 解耦。kb/cms 如需使用默认文件管理、配置读取、用户上下文与后台管理能力，请在应用中显式引入 `admin-starter`，或提供等价 Bean 实现

## 相关文档

- [完整架构说明](../docs/STARTER_ARCHITECTURE.md)
- [Admin 认证与账号安全](../docs/admin-auth-security.md)
- [权限控制设计](../docs/backend_permission_control_design.md)
- [API 分组指南](../docs/API_TAG_GROUP_GUIDE.md)

