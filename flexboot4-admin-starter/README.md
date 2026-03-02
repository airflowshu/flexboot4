# Flexboot4 Admin Starter

Admin Starter 提供了 flexboot4 的核心 RBAC（基于角色的访问控制）功能，包括：

## 功能特性

- **用户管理**：用户注册、登录、权限管理
- **角色管理**：角色定义、权限分配
- **菜单管理**：动态菜单、权限控制
- **部门管理**：组织架构管理
- **操作日志**：系统操作审计
- **登录日志**：用户登录追踪
- **安全认证**：JWT Token 认证
- **Redis 缓存**：高性能缓存支持
- **数据权限**：基于注解的数据权限控制
- **API 文档**：集成 SpringDoc（Scalar UI）

## 依赖

- Spring Boot 3.4.x
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

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",
    "com.example.yourapp"
})
@EnableCaching
@MapperScan({"com.yunlbd.flexboot4.mapper", "com.example.yourapp.mapper"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
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
  jwt:
    secret: your-secret-key-at-least-256-bits-long
    expiration: 86400000 # 24小时
```

## 核心 API

引入 Admin Starter 后，你可以使用以下服务：

```java
@Autowired
private SysUserService userService;

@Autowired
private SysRoleService roleService;

@Autowired
private SysMenuService menuService;

@Autowired
private SysDeptService deptService;
```

## 注解使用

### 权限控制

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping
    public List<SysUser> list() {
        // ...
    }
    
    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public void add(@RequestBody SysUser user) {
        // ...
    }
}
```

### 数据权限

```java
@Service
public class YourService {
    
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<YourEntity> list() {
        // 自动根据用户数据权限过滤
    }
}
```

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

## 扩展开发

如果需要扩展 Admin Starter 的功能，可以：

1. **添加自定义 Controller**：在自己的包下创建 Controller
2. **扩展实体类**：继承 Admin Starter 的实体类并添加字段
3. **自定义认证逻辑**：实现 `UserDetailsService` 接口

## 注意事项

1. Admin Starter 是纯库模块，不包含 `application.yml` 等配置文件
2. 外部项目需要配置数据库连接、Redis 连接等信息
3. 确保数据库已创建相应的表结构（可参考 `doc/sql/` 目录）
4. JWT Secret 建议使用至少 256 位的随机字符串

## 相关文档

- [完整架构说明](../STARTER_ARCHITECTURE.md)
- [权限控制设计](../doc/backend_permission_control_design.md)
- [API 分组指南](../doc/API_TAG_GROUP_GUIDE.md)

