# Admin 认证与账号安全

本文说明 Admin Starter 当前登录认证、个人中心安全设置与 MFA 部署配置。相关能力由 `flexboot4-admin-starter` 提供，前端入口位于 `/profile`、`/auth/login` 与 `/auth/oauth/callback`。

## 1. 登录方式

后台登录接口统一为：

```text
POST /api/admin/auth/login
```

当前支持以下第一阶段登录或认证入口：

| 登录方式 | 说明 | 前端入口控制 |
| --- | --- | --- |
| 账号密码登录 | 使用 `username + password` 完成第一阶段登录 | `auth.login.options.methods.password.enabled` |
| 手机验证码登录 | 使用已绑定的 `sys_user.phone` 与短信验证码登录 | `auth.login.options.methods.sms.enabled` |
| GitHub / QQ 第三方登录 | 使用第三方 OAuth 完成外部身份认证，再绑定或登录本地系统账号 | `auth.login.options.methods.thirdParty.providers[].enabled` |

短信登录是否展示与是否可用由系统配置 `auth.login.options` 控制。用户在个人中心绑定手机号后，不会自动开启短信登录入口；管理员仍需要显式开启短信登录配置。

第三方登录的详细集成、GitHub / QQ 配置、账号绑定规则与扩展方式见 [第三方登录集成与使用](admin-social-login.md)。

## 2. 手机验证码登录流程与限流

手机验证码登录分为“发送验证码”和“使用验证码登录”两步。

发送验证码接口：

```text
POST /api/admin/auth/sms-code
```

请求体：

```json
{
  "phone": "13800138000"
}
```

使用验证码登录仍走统一登录接口：

```text
POST /api/admin/auth/login
```

请求体：

```json
{
  "loginType": "sms",
  "phone": "13800138000",
  "code": "123456"
}
```

发送验证码流程：

1. 读取 `auth.login.options`，确认短信登录已启用。
2. 规范化并校验手机号格式。
3. 执行 IP 维度限流，防止同一 IP 横向请求多个手机号消耗短信额度。
4. 执行手机号维度冷却与每日次数限制。
5. 查询未删除、启用状态且绑定该手机号的用户。
6. 如果手机号未绑定任何用户，接口仍返回“验证码已发送，请注意查收”，但不会调用短信发送器。
7. 如果手机号已绑定用户，生成验证码并将哈希值写入 Redis，随后调用短信发送器。

验证码登录校验流程：

1. 校验手机号格式和验证码是否为空。
2. 检查该手机号验证码错误次数，默认最多允许 5 次错误尝试。
3. 校验 Redis 中保存的验证码哈希。
4. 验证码错误时只增加失败计数，不删除仍在有效期内的验证码。
5. 验证码正确且用户存在时登录成功，并清理验证码与失败计数。
6. 若用户已启用 MFA，第一阶段校验通过后返回 MFA challenge，不直接签发正式 `accessToken`。

短信验证码相关系统配置：

| 配置项 | 默认值 | 类型 | 说明 |
| --- | --- | --- | --- |
| `auth.login.options` | 见迁移脚本 | `JSON` | 控制登录方式开关，`methods.sms.enabled` 控制短信登录是否启用 |
| `auth.login.options.methods.sms.codeLength` | `6` | `NUMBER` | 验证码长度，运行时会限制在 4 到 8 位之间 |
| `auth.login.options.methods.sms.cooldownSeconds` | `60` | `NUMBER` | 同一手机号发送冷却秒数 |
| `auth.sms.templateId` | `1` | `STRING` | 短信模板 ID |
| `auth.sms.configId` | 空字符串 | `STRING` | 指定 sms4j 配置 ID，留空使用默认启用配置 |
| `auth.sms.ipHourlyLimit` | `30` | `NUMBER` | 同一 IP 每小时最多请求短信验证码次数 |
| `auth.sms.ipDailyLimit` | `100` | `NUMBER` | 同一 IP 每日最多请求短信验证码次数 |

短信验证码相关 Redis key：

| Key 前缀 | 默认 TTL | 说明 |
| --- | --- | --- |
| `auth:sms-code:` | 5 分钟 | 按手机号保存验证码哈希 |
| `auth:sms-code:cooldown:` | 由 `cooldownSeconds` 决定 | 同一手机号发送冷却 |
| `auth:sms-code:daily:` | 1440 分钟 | 同一手机号每日请求计数，默认每日最多 10 次 |
| `auth:sms-code:fail:` | 5 分钟 | 同一手机号验证码错误次数，默认最多 5 次 |
| `auth:sms-code:ip:hour:` | 60 分钟 | 同一 IP 每小时请求计数 |
| `auth:sms-code:ip:daily:` | 1440 分钟 | 同一 IP 每日请求计数 |

错误语义：

- 发送过于频繁或达到次数上限时，返回普通业务失败，不返回 401。
- 验证码错误、过期或失败次数超限时，返回“验证码不正确或已过期”，不返回“登录已过期”。
- 不存在手机号不会真实发送短信，但返回值与正常发送保持一致，用于避免手机号枚举。

相关 PostgreSQL Flyway 迁移已合并到 Admin 基线：

```text
db/flexboot4-migration/admin/postgresql/V1000__admin_core_schema.sql
```

## 3. 个人中心安全设置

`/profile` 的“安全设置”包含以下绑定能力：

| 项目 | 存储字段/表 | 唯一性 | 说明 |
| --- | --- | --- | --- |
| 密保手机 | `sys_user.phone` | 未删除用户中唯一 | 绑定后可用于手机号验证码登录 |
| 备用邮箱 | `sys_user.email` | 未删除用户中唯一 | 绑定后可用于找回密码等邮件能力 |
| MFA 设备 | `sys_user_mfa` | v1 每个用户最多一个启用的 TOTP 设备 | 登录二次验证 |

手机号与邮箱都只在个人中心展示脱敏值，不向前端暴露完整值。

## 4. MFA 设备

MFA v1 使用标准 TOTP，不接入 Microsoft 账号体系。系统生成标准 `otpauth://` 二维码，兼容以下认证器：

- Microsoft Authenticator
- Google Authenticator
- Authy
- 1Password 等支持 TOTP 的工具

TOTP 参数固定为：

| 参数 | 值 |
| --- | --- |
| 算法 | `HmacSHA1` |
| 位数 | `6` |
| 周期 | `30` 秒 |
| 时间窗口 | `±1` 个周期 |
| issuer | `FlexBoot4` |
| label | `FlexBoot4:{accountName}` |

`accountName` 每次初始化绑定时生成，格式类似 `admin@A7K3P2`。后缀用于避免用户解绑后重新绑定时，Microsoft Authenticator 等认证器因为本地仍保留旧条目而提示“账户已存在”。

绑定流程：

1. 用户进入 `/profile`，打开“安全设置”。
2. 点击“MFA 设备”的“绑定”。
3. 后端生成 TOTP secret、二维码 URI 与手动密钥，但此时不启用 MFA。
4. 前端展示账户名、二维码与手动密钥；如果手机端认证器中已有旧的 FlexBoot4 条目，建议先删除旧条目后再扫码绑定。
5. 用户输入认证器中的 6 位动态码，校验通过后启用 MFA。

解绑流程：

1. 用户点击“MFA 设备”的“解绑”。
2. 输入当前密码与当前 TOTP 动态码。
3. 校验通过后解绑当前 MFA 设备。

解绑后，当前认证器不再用于登录二次验证；如需再次启用 MFA，需要重新扫码绑定并生成新的 TOTP secret。当前 v1 不提供“临时暂停后恢复旧绑定”的能力，这样可以避免设备丢失或 secret 泄露场景下继续复用旧密钥。

已启用 MFA 的账号，无论使用账号密码登录还是手机号验证码登录，第一阶段校验通过后都不会直接签发正式 `accessToken`。

## 5. 登录二阶段验证

启用 MFA 后，第一阶段登录成功时接口返回 challenge：

```json
{
  "mfaRequired": true,
  "mfaChallengeToken": "xxxx",
  "mfaMethods": ["totp"],
  "expiresIn": 300
}
```

此时响应不包含正式 `accessToken`，前端不会写入登录态。用户输入认证器动态码后调用：

```text
POST /api/admin/auth/mfa/verify
```

请求体：

```json
{
  "challengeToken": "xxxx",
  "code": "123456"
}
```

校验通过后才返回正式登录响应与 `accessToken`。

MFA challenge 使用 Redis 保存，默认 TTL 为 5 分钟，失败超过 5 次后失效。临时 key 前缀为：

```text
auth:mfa:challenge:
```

通常不需要手动清理；如开发调试时确需清理，只清理 `auth:mfa:challenge:*` 这一类临时登录挑战即可。

## 6. 后端配置

TOTP secret 不明文入库，会使用服务端密钥加密后写入 `sys_user_mfa.secret_ciphertext`。生产环境必须提供稳定的 MFA 加密密钥：

```yaml
flexboot4:
  security:
    mfa-secret-key: ${FLEXBOOT4_SECURITY_MFA_SECRET_KEY}
```

环境变量示例：

```powershell
$env:FLEXBOOT4_SECURITY_MFA_SECRET_KEY="replace-with-a-random-32-byte-or-longer-secret"
```

Linux / Docker 示例：

```bash
export FLEXBOOT4_SECURITY_MFA_SECRET_KEY='replace-with-a-random-32-byte-or-longer-secret'
```

生成随机密钥示例：

```bash
openssl rand -base64 32
```

注意事项：

- 不要把生产密钥提交到代码仓库。
- 同一环境的所有后端实例必须使用同一个 `FLEXBOOT4_SECURITY_MFA_SECRET_KEY`。
- 密钥必须跨重启、滚动发布和容器重建保持不变。
- 修改该密钥后，已绑定的 TOTP secret 将无法解密，用户需要重新绑定 MFA，除非先做专门的数据迁移。
- 开发环境若未配置该项，会回退到 `jwt.secret`；生产环境不建议依赖回退值。

Docker Compose 可使用 `.env` 注入：

```dotenv
FLEXBOOT4_SECURITY_MFA_SECRET_KEY=replace-with-a-random-32-byte-or-longer-secret
```

```yaml
services:
  flexboot4:
    environment:
      FLEXBOOT4_SECURITY_MFA_SECRET_KEY: ${FLEXBOOT4_SECURITY_MFA_SECRET_KEY}
```

## 7. 数据库与迁移

短信登录、IP 限流、MFA 与第三方登录绑定表均由 Admin Starter 内置 PostgreSQL Flyway 迁移维护：

```text
db/flexboot4-migration/admin/postgresql/V1000__admin_core_schema.sql
```

业务应用配置 `spring.datasource` 后，Admin Starter 会默认合并 FlexBoot4 模块迁移目录。相关配置：

```yaml
flexboot4:
  flyway:
    enabled: true
    database: postgresql
    auto-detect-modules: true
```

当前项目仍处于开发阶段，旧数据不要求兼容。如果开发库中存在不符合唯一性约束的数据，应优先通过 SQL 清理后再执行迁移。

## 8. 前端依赖

MFA 二维码展示依赖：

- `@vueuse/integrations`
- `qrcode`

本仓库已在 `apps/web-antd/package.json` 与 `pnpm-lock.yaml` 中声明并锁定依赖。首次拉取相关改动或切换新环境时，按项目既有流程执行：

```bash
pnpm install --frozen-lockfile
```

当前本地工作区如果已经执行过 `pnpm install`，不需要重复安装。
