# Admin 认证与账号安全

本文说明 Admin Starter 当前登录认证、个人中心安全设置与 MFA 部署配置。相关能力由 `flexboot4-admin-starter` 提供，前端入口位于 `/profile` 与 `/auth/login`。

## 1. 登录方式

后台登录接口统一为：

```text
POST /api/admin/auth/login
```

当前支持两类第一阶段登录：

| 登录方式 | 说明 | 前端入口控制 |
| --- | --- | --- |
| 账号密码登录 | 使用 `username + password` 完成第一阶段登录 | `auth.login.options.password.enabled` |
| 手机验证码登录 | 使用已绑定的 `sys_user.phone` 与短信验证码登录 | `auth.login.options.sms.enabled` |

短信登录是否展示与是否可用由系统配置 `auth.login.options` 控制。用户在个人中心绑定手机号后，不会自动开启短信登录入口；管理员仍需要显式开启短信登录配置。

## 2. 个人中心安全设置

`/profile` 的“安全设置”包含以下绑定能力：

| 项目 | 存储字段/表 | 唯一性 | 说明 |
| --- | --- | --- | --- |
| 密保手机 | `sys_user.phone` | 未删除用户中唯一 | 绑定后可用于手机号验证码登录 |
| 备用邮箱 | `sys_user.email` | 未删除用户中唯一 | 绑定后可用于找回密码等邮件能力 |
| MFA 设备 | `sys_user_mfa` | v1 每个用户最多一个启用的 TOTP 设备 | 登录二次验证 |

手机号与邮箱都只在个人中心展示脱敏值，不向前端暴露完整值。

## 3. MFA 设备

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

## 4. 登录二阶段验证

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

## 5. 后端配置

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

## 6. 数据库与迁移

MFA 使用 `sys_user_mfa` 表，Admin Starter 内置 PostgreSQL Flyway 迁移：

```text
db/migration/flexboot4/admin/postgresql/V6__sys_user_mfa.sql
```

如果业务应用希望自动纳入 Admin Starter 迁移目录，需要启用：

```yaml
flexboot4:
  flyway:
    admin-migrations-enabled: true
```

当前项目仍处于开发阶段，旧数据不要求兼容。如果开发库中存在不符合唯一性约束的数据，应优先通过 SQL 清理后再执行迁移。

## 7. 前端依赖

MFA 二维码展示依赖：

- `@vueuse/integrations`
- `qrcode`

本仓库已在 `apps/web-antd/package.json` 与 `pnpm-lock.yaml` 中声明并锁定依赖。首次拉取相关改动或切换新环境时，按项目既有流程执行：

```bash
pnpm install --frozen-lockfile
```

当前本地工作区如果已经执行过 `pnpm install`，不需要重复安装。
