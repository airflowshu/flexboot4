# 用户注销接口设计与实现方案

## 背景与需求

admin 需要增加一个用户注销的接口，方便执行用户的注销服务，以符合 GDPR/PIPL 等标准。

### 核心问题：为什么选择二步确认而非一次性调用？

#### 1. 法律合规性（GDPR/PIPL 核心要求）

**GDPR 第 17 条（被遗忘权）** 明确规定：
- 用户有权要求删除个人数据，但企业有合理的宽限期来验证请求的真实性
- 必须防止**误操作**和**未授权删除**

**PIPL（中国个人信息保护法）** 也要求：
- 用户删除请求必须有**确认机制**
- 要记录**完整的操作链路**便于审计

**一次性删除的风险：**
- ❌ 账户被盗用者可以直接删除目标用户账号
- ❌ 用户误点删除按钮无法悔改
- ❌ 无法证明"确实是用户本人主动发起的请求"

#### 2. 用户体验与悔改权

**二步流程：**
```
第一步：用户点击"申请注销" → 生成验证 Token → 发送邮件/短信
第二步：用户在邮件中点击确认链接/输入验证码 → 执行最终删除
```

**优点：**
- ✅ 给用户 24-48 小时的冷静期，防止冲动决定
- ✅ 如果邮箱被盗用，用户仍可通过其他方式撤回
- ✅ 用户可以改主意不点确认链接

**一次性删除：**
- ❌ 用户误点后秒删，无法恢复
- ❌ 可能导致法律纠纷（用户声称未授权删除）
- ❌ 数据已删，无法追溯

#### 3. 安全性考量

**攻击场景 1：CSRF 攻击**
```
场景：
- 用户登录 A 网站后，恶意网站 B 自动发送 DELETE /user/123
- 浏览器自动带上 Cookie，用户账户被删除

防护：
- 第一步：申请注销 → 生成独立的 deaccountToken（与 JWT 不同）
- 第二步：确认时必须提交 Token，CSRF 攻击者无法获得
```

**攻击场景 2：账户接管**
```
场景：
- 黑客入侵用户账号，直接调用 DELETE 删除账户
- 用户无法恢复数据

防护：
- 第一步生成的 Token 通过邮件/短信发送（黑客通常无法访问）
- 即使账号被盗，黑客也删不了账户
```

#### 4. 审计与追溯需求

**二步确认提供完整审计链：**
```json
操作日志 1（申请阶段）：
{
  "action": "deaccount_request",
  "userId": "123",
  "reason": "不想用了",
  "operIp": "192.168.1.100",
  "operTime": "2026-02-28 10:00:00",
  "status": "pending"
}

操作日志 2（确认阶段）：
{
  "action": "deaccount_confirm",
  "userId": "123",
  "confirmIp": "192.168.1.100",
  "confirmTime": "2026-02-28 10:15:00",
  "tokenUsed": "xxx_hash",
  "status": "confirmed"
}
```

**一次性删除：**
- 只有一条日志，无法区分"是否真的是用户主动操作"

#### 5. 实际案例对比

| 平台 | 实现方式 | 说明 |
|------|--------|------|
| **Google** | 二步确认 | 点击删除 → 邮件确认链接 → 30 天后执行 |
| **Apple** | 二步确认 | 申请删除 → 拨打客服电话 → 3 个月后执行 |
| **微信** | 立即删除 | 账户被盗用户多，被诟病 ❌ |
| **微博** | 二步确认 | 申请注销 → 邮件确认 → 7 天后执行 |
| **抖音** | 二步确认 | 申请注销 → 60 天后执行（可撤回） |

---

## 推荐方案对比

### 方案 A：一次性删除 + 密码确认 ✅ 适合内部系统
```
POST /api/user/deaccount
{
  "password": "user_password",
  "reason": "不想用了"
}

优点：
- 用户体验简单（一次点击）
- 安全性有保障（需密码验证）
- 适合内部系统或风险承受度高的场景

缺点：
- 如果密码也被盗，仍无法防护
```

### 方案 B：二步确认 + OTP（一次性密码）✅ 安全最优 | 推荐
```
第一步：POST /api/user/deaccount-request
  返回：requestId，向用户邮箱/短信发送 6 位 OTP

第二步：POST /api/user/deaccount-confirm
  {
    "requestId": "xxx",
    "otp": "123456"
  }
  执行删除

优点：
- 用户体验：只需两次点击/输入
- 安全性：OTP 时效短（5 分钟），成本低
- 法律合规：有完整审计链
```

### 方案 C：即时删除 + 邮件通知 ✅ 平衡方案
```
POST /api/user/deaccount
{
  "reason": "不想用了"
}

→ 立即删除用户
→ 发送邮件："你的账户已于 2026-02-28 10:00 删除，如非本人操作请在 24 小时内回复"

缺点：
- 事后通知不如事前确认安全
```

---

## 架构设计

### 1. 实体扩展

#### SysUser 新增字段
```java
// 注销相关字段
private Integer delFlag;           // 逻辑删除标记（已有）
private LocalDateTime deletedTime;  // 注销时间戳
private String deleteReason;        // 注销原因
private String deletedBy;           // 注销操作人（管理员或系统）
```

#### 新增 UserDeaccountRequest 实体
```java
@Table("sys_user_deaccount_request")
public class UserDeaccountRequest extends BaseEntity {
    private String userId;              // 用户 ID
    private String reason;              // 注销原因
    private String token;               // 确认令牌（哈希）
    private Integer status;             // 0: pending, 1: confirmed, 2: expired, 3: cancelled
    private LocalDateTime tokenExpireTime;  // Token 过期时间
    private LocalDateTime confirmedTime; // 确认时间
    private String confirmedIp;         // 确认 IP
    private String requestIp;           // 请求 IP
}
```

### 2. Service 层扩展

#### SysUserService 接口
```java
public interface SysUserService extends IExtendedService<SysUser> {
    
    boolean updatePasswordById(String id, String newPassword);
    
    /**
     * 第一步：申请用户注销
     * @param userId 用户 ID
     * @param reason 注销原因
     * @return 包含 requestId 的响应
     */
    UserDeaccountRequestDto requestDeaccount(String userId, String reason);
    
    /**
     * 第二步：确认用户注销
     * @param requestId 注销请求 ID
     * @param confirmToken 确认 Token（OTP 或邮件链接中的 Token）
     * @return 是否删除成功
     */
    boolean confirmDeaccount(String requestId, String confirmToken);
    
    /**
     * 取消注销请求（在冷却期内）
     * @param requestId 注销请求 ID
     * @return 是否取消成功
     */
    boolean cancelDeaccount(String requestId);
    
    /**
     * 执行用户数据删除（级联删除相关数据）
     * @param userId 用户 ID
     * @param deletedBy 操作人
     */
    void executeUserDeaccount(String userId, String deletedBy);
}
```

### 3. DTO 定义

#### UserDeaccountRequestDto
```java
public class UserDeaccountRequestDto {
    private String requestId;           // 注销请求 ID
    private String userId;              // 用户 ID
    private String message;             // 提示信息
    private Integer status;             // 请求状态
    private LocalDateTime expireTime;   // Token 过期时间
}
```

#### UserDeaccountConfirmDto
```java
public class UserDeaccountConfirmDto {
    private String requestId;
    private String otp;  // 或 token，取决于实现方式
}
```

### 4. Controller 层

#### SysUserController 新增端点

**端点 1：申请注销**
```
POST /api/admin/user/deaccount-request

请求体：
{
  "reason": "不想用了"
}

响应：
{
  "code": 200,
  "message": "注销申请已提交，请检查邮箱确认",
  "data": {
    "requestId": "1234567890",
    "expireTime": "2026-03-01 10:00:00"
  }
}
```

**端点 2：确认注销**
```
POST /api/admin/user/deaccount-confirm

请求体：
{
  "requestId": "1234567890",
  "otp": "123456"
}

响应：
{
  "code": 200,
  "message": "账户已注销，30 天后完全删除"
}
```

**端点 3：取消注销（可选）**
```
POST /api/admin/user/deaccount-cancel

请求体：
{
  "requestId": "1234567890"
}

响应：
{
  "code": 200,
  "message": "注销申请已取消"
}
```

### 5. 事件驱动架构

#### 事件定义

```java
// 注销申请事件
public class UserDeaccountRequestEvent extends ApplicationEvent {
    private UserDeaccountRequest deaccountRequest;
    private SysUser user;
}

// 注销确认事件
public class UserDeaccountConfirmedEvent extends ApplicationEvent {
    private String userId;
    private UserDeaccountRequest deaccountRequest;
}

// 注销执行事件
public class UserDeaccountExecutedEvent extends ApplicationEvent {
    private String userId;
    private LocalDateTime executedTime;
}
```

#### 事件监听器

```java
// 1. 邮件通知监听器
@Component
@EventListener
public void onDeaccountRequest(UserDeaccountRequestEvent event) {
    // 发送邮件包含 OTP 或确认链接
}

// 2. 级联数据清理监听器
@Component
@EventListener
public void onDeaccountConfirmed(UserDeaccountConfirmedEvent event) {
    // 删除用户角色关系
    // 删除用户文件关系
    // 删除用户 Token
    // 调用 Hook 机制通知第三方模块
}

// 3. 审计日志监听器
@Component
@EventListener
public void onDeaccountExecuted(UserDeaccountExecutedEvent event) {
    // 记录最终删除日志
}
```

#### 可扩展 Hook 机制

```java
// Hook 接口定义
public interface UserDeaccountHook {
    String getModuleName();  // 模块名：kb, media 等
    void onDeaccountConfirmed(String userId);  // 在确认阶段调用
    void onDeaccountExecuted(String userId);   // 在执行删除前调用
}

// 在监听器中调用所有 Hook
@Component
@RequiredArgsConstructor
public class UserDeaccountListener {
    private final List<UserDeaccountHook> hooks;
    
    @EventListener
    public void onDeaccountConfirmed(UserDeaccountConfirmedEvent event) {
        for (UserDeaccountHook hook : hooks) {
            try {
                hook.onDeaccountConfirmed(event.getUserId());
            } catch (Exception e) {
                log.error("Hook {} failed: {}", hook.getModuleName(), e.getMessage());
            }
        }
    }
}

// KB 模块实现 Hook
@Component
public class KbUserDeaccountHook implements UserDeaccountHook {
    @Override
    public String getModuleName() {
        return "kb";
    }
    
    @Override
    public void onDeaccountConfirmed(String userId) {
        // 删除用户在知识库中的权限、上传文件等
    }
}
```

### 6. 权限控制

```java
// 权限码定义
sys:user:deaccount:request   // 申请注销权限
sys:user:deaccount:confirm   // 确认注销权限
sys:user:deaccount:cancel    // 取消注销权限

// Controller 注解
@PostMapping("/deaccount-request")
@RequirePermission("sys:user:deaccount:request")
@OperLog(title = "申请用户注销", businessType = BusinessType.OTHER)
public ApiResult<UserDeaccountRequestDto> requestDeaccount(...) { }

@PostMapping("/deaccount-confirm")
@RequirePermission("sys:user:deaccount:confirm")
@OperLog(title = "确认用户注销", businessType = BusinessType.DELETE)
public ApiResult<Boolean> confirmDeaccount(...) { }
```

### 7. 操作日志记录

```java
// 注销申请日志
{
  "action": "deaccount_request",
  "title": "申请用户注销",
  "businessType": "OTHER",
  "userId": "123",
  "operParam": {
    "reason": "不想用了"
  },
  "operIp": "192.168.1.100",
  "operTime": "2026-02-28 10:00:00"
}

// 注销确认日志
{
  "action": "deaccount_confirm",
  "title": "确认用户注销",
  "businessType": "DELETE",
  "userId": "123",
  "operParam": {
    "requestId": "1234567890"
  },
  "operIp": "192.168.1.100",
  "operTime": "2026-02-28 10:15:00",
  "status": "0"  // 0 成功，1 异常
}
```

### 8. 数据清理策略

#### 级联删除清单

1. **sys_user** - 逻辑删除（设置 delFlag=1）
2. **sys_user_role** - 物理删除（删除所有角色关系）
3. **sys_user_token** - 物理删除（如有 Token 表，删除所有 Token）
4. **sys_file**（如有用户文件关系）- 物理删除
5. **sys_oper_log**（可选）- 脱敏处理（将 operUserId、operName 设为 NULL）
6. **KB 模块相关表** - 由 KB 模块通过 Hook 处理
7. **Media 模块相关表** - 由 Media 模块通过 Hook 处理

#### 数据保留决策

```
需要物理删除：
- 用户密码
- 用户个人信息（邮箱、手机、真名等）
- 用户上传的文件内容
- 用户与角色的关系

可以脱敏保留（GDPR 允许）：
- 操作日志（替换用户身份信息）
- 审计记录（用于未来合规审查）
- 登录日志（用于安全分析）
```

### 9. 冷却期与定时任务

```java
// 配置冷却期
deaccount.cooling-period-days=7  // 7 天冷却期

// 定时任务：每天检查过期的注销请求
@Component
@RequiredArgsConstructor
public class UserDeaccountScheduler {
    
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点执行
    public void executeExpiredDeaccountRequests() {
        // 查询所有 status=1 且 confirmedTime + coolingPeriod < now 的请求
        // 调用 executeUserDeaccount 执行删除
        // 发送删除完成邮件
    }
}
```

---

## 实现步骤

### Phase 1：数据层（Week 1）
- [ ] 创建 `UserDeaccountRequest` 实体
- [ ] 创建 `UserDeaccountRequestMapper` 和 `UserDeaccountRequestService`
- [ ] 修改 `SysUser` 实体，添加注销字段
- [ ] 创建数据库迁移脚本（Flyway）

### Phase 2：服务层（Week 1-2）
- [ ] 实现 `SysUserService` 接口的三个方法
- [ ] 实现 OTP 生成与验证逻辑
- [ ] 实现邮件发送集成
- [ ] 创建事件类

### Phase 3：控制层（Week 2）
- [ ] 添加三个 Controller 端点
- [ ] 实现权限控制
- [ ] 配置操作日志

### Phase 4：事件与监听（Week 2-3）
- [ ] 实现事件监听器链
- [ ] 实现 Hook 接口和 KB/Media 模块集成
- [ ] 实现定时任务（冷却期执行）

### Phase 5：测试与文档（Week 3-4）
- [ ] 单元测试与集成测试
- [ ] API 文档（OpenAPI）
- [ ] 用户指南与管理员指南

---

## 关键考虑点

### 1. OTP/Token 生成与存储

```java
// 不要明文存储 Token
// 正确做法：存储 Token 的 SHA256 Hash
String token = generateRandomToken();  // 36 位随机字符串
String tokenHash = DigestUtils.sha256Hex(token);
deaccountRequest.setToken(tokenHash);

// 发送时只发送明文 token（一次性）
// 验证时对比 hash：DigestUtils.sha256Hex(inputToken).equals(storedHash)
```

### 2. Token 过期时间

```
OTP 过期：5 分钟（防止猜测）
确认 Token 过期：24-48 小时（给用户充足时间）
整体冷却期：7-30 天（GDPR 推荐）
```

### 3. 幂等性保证

```java
// 同一个 requestId 确认多次应该是安全的
@PostMapping("/deaccount-confirm")
public ApiResult<Boolean> confirmDeaccount(UserDeaccountConfirmDto dto) {
    UserDeaccountRequest request = getById(dto.getRequestId());
    
    // 如果已确认，直接返回成功
    if (request.getStatus() == 1) {
        return ApiResult.success(true);
    }
    
    // 验证 Token...
}
```

### 4. 日志脱敏

```java
// 删除后的日志中不应包含用户隐私信息
@OperLog
@PostMapping("/deaccount-confirm")
public ApiResult<Boolean> confirmDeaccount(...) {
    // operParam 中不要记录原始密码、邮箱等
    // 只记录：requestId、结果状态
}
```

### 5. 异常处理与重试

```java
// 邮件发送失败应该不影响主流程
@EventListener
public void onDeaccountRequest(UserDeaccountRequestEvent event) {
    try {
        emailService.sendOtp(event.getUser().getEmail(), otp);
    } catch (Exception e) {
        log.error("Failed to send OTP email, retrying...");
        // 重试或降级处理（如短信备用）
    }
}
```

---

## 依赖项

```gradle
// OTP 生成
implementation 'com.google.zxing:core:3.5.0'

// 邮件发送（如果不存在）
implementation 'org.springframework.boot:spring-boot-starter-mail'

// 定时任务
// Spring 已内置 @Scheduled 支持
```

---

## 配置示例

```yaml
# application.yml
deaccount:
  otp:
    length: 6
    expiry-minutes: 5
    max-attempts: 3
  
  token:
    expiry-hours: 48
  
  cooling-period-days: 7
  
  # 邮件配置（现有）
  email:
    subject: "确认账户注销"
    template: "deaccount-confirmation"

# 权限配置
permissions:
  sys:user:deaccount:request: "ROLE_ADMIN"
  sys:user:deaccount:confirm: "ROLE_ADMIN"
  sys:user:deaccount:cancel: "ROLE_ADMIN,ROLE_USER_SELF"
```

---

## API 示例

### 示例 1：用户申请注销

```bash
curl -X POST http://localhost:8080/api/admin/user/deaccount-request \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "不再需要此账户"
  }'

# 响应
{
  "code": 200,
  "message": "注销申请已提交，请检查邮箱确认",
  "data": {
    "requestId": "1846524632901652480",
    "expireTime": "2026-03-02 10:30:00"
  }
}
```

### 示例 2：用户确认注销

```bash
curl -X POST http://localhost:8080/api/admin/user/deaccount-confirm \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "requestId": "1846524632901652480",
    "otp": "123456"
  }'

# 响应
{
  "code": 200,
  "message": "账户已确认删除，将在 7 天后完全删除",
  "data": true
}
```

### 示例 3：邮件内容

```
尊敬的用户：

您在 2026-02-28 10:00 申请注销账户。

为了您的账户安全，请在 48 小时内通过以下方式之一确认：

方式 1：点击链接确认（推荐）
https://example.com/confirm-deaccount?token=xxx&requestId=1846524632901652480

方式 2：输入验证码
验证码：123456
有效期：5 分钟

如果您没有申请注销账户，请忽略此邮件。

注意：确认后，您的账户将在 7 天后完全删除，此操作不可撤销。
```

---

## 测试场景

### 单元测试
- [ ] OTP 生成与验证
- [ ] Token 哈希与比对
- [ ] 时间过期判断
- [ ] 级联删除逻辑

### 集成测试
- [ ] 申请注销流程
- [ ] 邮件发送验证
- [ ] 确认注销流程
- [ ] Hook 机制触发
- [ ] 定时任务执行

### 压力测试
- [ ] 大量并发注销请求
- [ ] 邮件队列性能
- [ ] 定时任务效率

---

## 监控与告警

```java
// Metrics 指标
- deaccount.requests.total        // 总申请数
- deaccount.confirms.total        // 总确认数
- deaccount.cancels.total         // 总取消数
- deaccount.executions.total      // 总执行数
- deaccount.confirmation.latency  // 确认耗时（分钟）

// 告警条件
- 同一用户 24 小时申请超过 5 次（可能是滥用）
- OTP 验证失败超过 3 次（可能是破解尝试）
- Hook 执行失败（需要人工介入）
- 定时任务失败（需要重试）
```

---

## 总结

该方案提供了一个**符合 GDPR/PIPL 合规要求**的用户注销接口实现，具有以下特点：

✅ **安全性**：二步确认 + OTP 验证 + Token 哈希存储
✅ **法律合规**：冷却期 + 完整审计链 + 数据清理策略
✅ **可扩展性**：Hook 机制支持第三方模块集成
✅ **可靠性**：事件驱动 + 异步处理 + 幂等性保证
✅ **用户友好**：邮件通知 + 撤回机制 + 7-30 天冷却期

根据实际项目情况，可选择**简化版本**（方案 A：密码确认）或**完整版本**（方案 B：OTP 确认）。

