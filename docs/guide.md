# 接入指南

## 🚀 5 分钟快速开始

### Step 1: 添加依赖

在 `build.gradle.kts` 中：

```kotlin
dependencies {
    // 使用 BOM 统一版本管理
    implementation(platform("com.yunlbd:flexboot4-bom:0.0.1-SNAPSHOT"))

    // 选择你需要的 Starter
    implementation("com.yunlbd:flexboot4-admin-starter")
    // implementation("com.yunlbd:flexboot4-kb-starter")      // 可选：知识库
    // implementation("com.yunlbd:flexboot4-media-starter")   // 可选：媒体处理
    // implementation("com.yunlbd:flexboot4-sms4j-starter")   // 可选：短信能力
}
```

### Step 2: 创建启动类

```java
package com.example.yourapp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = {
    "com.yunlbd.flexboot4",      // ✅ 扫描 FlexBoot4 组件
    "com.example.yourapp"         // 扫描自己的组件
})
@EnableCaching
@MapperScan({
    "com.yunlbd.flexboot4.mapper",
    "com.example.yourapp.mapper"
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### Step 3: 配置数据库（PostgreSQL）

在 `application.yml` 中：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flexboot4
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update

# 其他配置...
```

### Step 4: 启动应用

```bash
./gradlew bootRun
```

访问 API 文档：http://localhost:8080/scalar

---

## 📦 模块选择指南

| 需求场景 | 推荐方案 | 说明 |
|---------|--------|------|
| **仅需用户管理 + RBAC** | `admin-starter` | 包含用户、角色、菜单、权限管理 |
| **需要知识库 + RAG** | `kb-starter` | 自动传递 `admin-starter`，支持文档解析与向量化 |
| **需要媒体处理** | `media-starter` | 自动传递 `admin-starter`，支持视频/音频处理 |
| **需要短信能力** | `sms4j-starter` | 自动传递 `admin-starter`，支持短信厂商配置与动态刷新 |
| **全功能平台** | 所有 Starter | Admin + KB + Media + SMS 完整功能 |

### 依赖自动传递

```
flexboot4-core (纯 Java 基础库)
    ↓
flexboot4-admin-starter (RBAC 内核)
    ↓
├── flexboot4-kb-starter (依赖 admin-starter)
├── flexboot4-media-starter (依赖 admin-starter)
└── flexboot4-sms4j-starter (依赖 admin-starter)
```

✅ **只需引入最高层的 Starter，低层依赖自动传递**

---

## 🎯 核心功能速览

### 1. RBAC 权限管理

#### 系统已预置的数据表
```
sys_user          # 用户表
sys_role          # 角色表
sys_user_role     # 用户-角色关联表
sys_menu          # 菜单表
sys_role_menu     # 角色-菜单关联表
sys_permission    # 权限编码表
```

#### 快速创建用户

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends BaseController<SysUserService, SysUser, String> {

    private final PasswordEncoder passwordEncoder;

    @PostMapping
    @OperLog(title = "新建用户", businessType = BusinessType.INSERT)
    public ApiResult<Boolean> createUser(@RequestBody SysUser user) {
        user.setPassword(passwordEncoder.encode("123456"));
        return ApiResult.success(service.save(user));
    }
}
```

#### 权限控制注解

```java
@PostMapping("/sensitive-operation")
@RequirePermission("sys:user:delete")  // 需要删除权限
public ApiResult<Boolean> deleteUser(@RequestParam String userId) {
    return ApiResult.success(service.removeById(userId));
}
```

### 2. 通用查询 API

所有 Controller 自动支持 `/page` 和 `/list` 端点，支持复杂查询：

#### 单表查询

```bash
curl -X POST http://localhost:8080/api/admin/user/page \
  -H "Content-Type: application/json" \
  -d '{
    "pageNumber": 1,
    "pageSize": 10,
    "items": [
      { "field": "status", "op": "eq", "val": 1 },
      { "field": "realName", "op": "like", "val": "张三" }
    ],
    "orders": [
      { "column": "createTime", "asc": false }
    ]
  }'
```

#### 多表联合查询

```json
{
  "pageNumber": 1,
  "pageSize": 10,
  "items": [
    { "field": "dept.deptName", "op": "like", "val": "技术部" }
  ]
}
```

#### 嵌套条件查询

```json
{
  "items": [
    { "field": "status", "op": "eq", "val": 1 },
    {
      "logic": "OR",
      "children": [
        { "field": "type", "op": "eq", "val": "A" },
        { "field": "type", "op": "eq", "val": "B" }
      ]
    }
  ]
}
```

### 3. 操作日志（动态分表）

系统自动按季度分表存储操作日志，支持跨季度查询：

```
sys_oper_log_2026_q1  # 第一季度
sys_oper_log_2026_q2  # 第二季度
...
```

#### 查询操作日志

```bash
curl -X POST http://localhost:8080/api/oper-log/page \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "pageNumber": 1,
    "pageSize": 10,
    "items": [
      { "field": "operName", "op": "like", "val": "admin" }
    ]
  }'
```

#### 自动记录操作

```java
@PostMapping
@OperLog(title = "新建用户", businessType = BusinessType.INSERT)
public ApiResult<Boolean> create(@RequestBody SysUser user) {
    return ApiResult.success(service.save(user));
}

// 自动记录：操作人、IP、User-Agent、执行时间、请求参数、响应数据等
```

### 4. 数据脱敏

```java
// 自动脱敏 8 种敏感信息
// 手机、身份证、银行卡、邮箱、地址、姓名、车牌、IP

@GetMapping("/{id}")
public ApiResult<SysUser> getUserInfo(@PathVariable String id) {
    return ApiResult.success(service.getById(id));
}

// 响应自动脱敏敏感字段
```

### 5. 知识库 & RAG（可选）

```gradle
dependencies {
    implementation("com.yunlbd:flexboot4-kb-starter")
}
```

#### 上传文档

```bash
curl -X POST http://localhost:8080/api/kb/documents/upload \
  -F "file=@/path/to/document.pdf" \
  -H "Authorization: Bearer ${TOKEN}"
```

#### 文档智能检索

```bash
curl -X POST http://localhost:8080/api/kb/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "如何使用 FlexBoot4？",
    "topK": 5
  }'
```

### 6. AI 网关（独立部署）

#### 独立启动 AI 网关

```bash
./gradlew :flexboot4-ai:bootRun
```

访问：http://localhost:9090

#### AI 网关特性

- ✅ 高性能 WebFlux（Reactor 响应式）
- ✅ 离线 API Key 鉴权（无需 RPC）
- ✅ 流式响应支持（SSE）
- ✅ 配额管理与限流
- ✅ 日志汇聚（Redis Stream）

### 7. SMS4J 短信模块（可选）

```gradle
dependencies {
    implementation("com.yunlbd:flexboot4-sms4j-starter")
}
```

#### 初始化配置表

```bash
psql -U postgres -d flexboot4 -f docs/sql/sms4j_config_pg.sql
```

#### 短信厂商配置管理接口

- `POST /api/admin/sms/config`
- `PUT /api/admin/sms/config/{id}`
- `POST /api/admin/sms/config/page`

更多见：[SMS4J Starter 接入说明](./SMS4J_STARTER.md)

---

## 🔌 自定义业务 Controller

基于 FlexBoot4，快速开发自己的业务 Controller：

### 创建实体

```java
import com.mybatisflex.annotation.Table;
import com.yunlbd.flexboot4.entity.sys.BaseEntity;

@Table("t_product")
public class Product extends BaseEntity {
    private String name;
    private BigDecimal price;
    private String description;
}
```

### 创建 Mapper

```java
import com.mybatisflex.core.BaseMapper;

public interface ProductMapper extends BaseMapper<Product> {
}
```

### 创建 Service

```java
import com.yunlbd.flexboot4.service.sys.IExtendedService;

public interface ProductService extends IExtendedService<Product> {
}

@Service
@CacheConfig(cacheNames = "product")
public class ProductServiceImpl extends BaseServiceImpl<ProductMapper, Product> 
    implements ProductService {
}
```

### 创建 Controller

```java
import com.yunlbd.flexboot4.controller.sys.BaseController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "产品管理")
public class ProductController extends BaseController<ProductService, Product, String> {

    @Override
    public Class<Product> getEntityClass() {
        return Product.class;
    }

    // 自动继承：GET /{id}, POST, PUT /{id}, DELETE /{id}, POST /page, POST /list 等
    // 可按需添加自定义方法

    @PostMapping("/batch-update-price")
    @RequirePermission("product:update")
    @OperLog(title = "批量更新产品价格", businessType = BusinessType.UPDATE)
    public ApiResult<Boolean> batchUpdatePrice(@RequestBody List<Product> products) {
        return ApiResult.success(service.updateBatch(products));
    }
}
```

### 自动获得的能力

```
✅ CRUD 操作        (Create, Read, Update, Delete)
✅ 分页查询          (/page)
✅ 列表查询          (/list)
✅ 批量操作          (/batch)
✅ 复杂查询构建      (SearchDto 支持)
✅ 权限控制          (@RequirePermission)
✅ 操作日志          (@OperLog)
✅ 缓存管理          (@CacheConfig)
✅ OpenAPI 文档      (自动生成)
```

---

## 🔧 常见配置

### application.yml

```yaml
spring:
  application:
    name: my-flexboot4-app
  
  datasource:
    url: jdbc:postgresql://localhost:5432/flexboot4
    username: postgres
    password: password
  
  redis:
    host: localhost
    port: 6379
    password: ''
  
  mail:
    host: smtp.qq.com
    port: 465
    username: your-email@qq.com
    password: your-password
  
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: true

# FlexBoot4 配置
flexboot4:
  # JWT 配置
  jwt:
    secret: your-secret-key
    expiration: 86400  # 1 day
  
  # 权限配置
  permission:
    enabled: true
  
  # 操作日志配置
  oper-log:
    enabled: true
    cooling-period-days: 90  # 日志保留期
  
  # 数据脱敏配置
  desensitization:
    enabled: true
```

---

## 📚 项目结构

```
your-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/yourapp/
│   │   │       ├── YourApplication.java       # 启动类
│   │   │       ├── entity/
│   │   │       │   └── Product.java
│   │   │       ├── mapper/
│   │   │       │   └── ProductMapper.java
│   │   │       ├── service/
│   │   │       │   ├── ProductService.java
│   │   │       │   └── impl/
│   │   │       │       └── ProductServiceImpl.java
│   │   │       └── controller/
│   │   │           └── ProductController.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/example/yourapp/
│               └── ProductControllerTest.java
└── build.gradle.kts
```

---

## 🧪 测试示例

### Controller 测试

```java
@SpringBootTest
class ProductControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCreateProduct() {
        Product product = Product.builder()
            .name("产品1")
            .price(BigDecimal.valueOf(99.99))
            .build();

        ResponseEntity<ApiResult<Boolean>> response = restTemplate.postForEntity(
            "/api/products",
            product,
            new ParameterizedTypeReference<ApiResult<Boolean>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    void testPageQuery() {
        SearchDto searchDto = SearchDto.builder()
            .pageNumber(1)
            .pageSize(10)
            .build();

        ResponseEntity<ApiResult<Page<Product>>> response = restTemplate.postForEntity(
            "/api/products/page",
            searchDto,
            new ParameterizedTypeReference<ApiResult<Page<Product>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

## 🆘 常见问题

### Q: 如何自定义权限码？

**A:** 在数据库中添加权限记录：

```sql
INSERT INTO sys_permission (id, permission_code, permission_name) 
VALUES ('1', 'product:export', '导出产品');
```

然后在 Controller 中使用：

```java
@PostMapping("/export")
@RequirePermission("product:export")
public void export(HttpServletResponse response) { ... }
```

### Q: 如何扩展操作日志字段？

**A:** 继承 `SysOperLog` 或在事件监听器中自定义：

```java
@Component
@EventListener
public void onOperLog(SysOperLogEvent event) {
    SysOperLog log = event.getSysOperLog();
    // 添加自定义字段
    log.setRemark("自定义备注");
}
```

### Q: 如何在第三方模块中接收事件？

**A:** 实现 `Hook` 接口：

```java
@Component
public class MyCustomHook implements UserDeaccountHook {
    @Override
    public String getModuleName() {
        return "my-module";
    }

    @Override
    public void onDeaccountConfirmed(String userId) {
        // 在用户注销时执行自定义逻辑
    }
}
```

---

## 📖 更多文档

- [Starter 架构设计](./STARTER_ARCHITECTURE.md)
- [权限控制实现](./backend_permission_control_design.md)
- [用户注销方案](../plan-userDeaccount.prompt.md)
- [快速参考](./QUICKSTART.md)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 许可证

Apache License 2.0

