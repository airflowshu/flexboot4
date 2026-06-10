# 第三方登录集成与使用

本文说明 Admin Starter 的第三方登录集成方式。当前支持 GitHub 与 QQ，后续接入其他第三方厂商时复用同一套后端 OAuth 回调、本地账号绑定、本地 JWT 登录模型。

## 1. 设计边界

第三方平台只作为身份认证来源，不直接决定系统权限。用户通过 GitHub 或 QQ 授权后，系统仍然只根据本地 `sys_user`、角色、菜单和权限码完成登录态与授权控制。

当前实现遵循以下规则：

- 不自动创建系统账号。
- 不根据第三方邮箱或 OpenID 自动绑定系统账号。
- GitHub verified email 只用于提示候选账号；QQ 不返回邮箱，因此不会产生邮箱候选。
- 首次第三方登录未绑定时，用户必须输入已有系统账号和密码完成绑定。
- 登录成功后签发本地 `LoginResp` 与本地 `accessToken`，前端继续按现有登录完成逻辑拉取用户信息和权限码。

## 2. Provider 配置

### 2.1 GitHub OAuth App

在 GitHub 创建 OAuth App，回调地址填写后端 OAuth callback 地址：

```text
http://localhost:8080/api/admin/auth/oauth/github/callback
```

部署环境按实际后端公网地址填写，例如：

```text
https://api.example.com/api/admin/auth/oauth/github/callback
```

GitHub OAuth App 的 callback URL 必须指向后端接口，不是前端 `/auth/oauth/callback` 页面。前端回调页由后端在处理完 GitHub callback 后再跳转。

后端读取受保护配置，不从公开系统配置表读取 GitHub secret：

```powershell
$env:FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_ID="your-github-client-id"
$env:FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_SECRET="your-github-client-secret"
$env:FLEXBOOT4_AUTH_OAUTH_GITHUB_CALLBACK_URI="http://localhost:8080/api/admin/auth/oauth/github/callback"
```

Linux / Docker 示例：

```bash
export FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_ID='your-github-client-id'
export FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_SECRET='your-github-client-secret'
export FLEXBOOT4_AUTH_OAUTH_GITHUB_CALLBACK_URI='http://localhost:8080/api/admin/auth/oauth/github/callback'
```

对应 Spring Boot 配置前缀为：

```yaml
flexboot4:
  auth:
    oauth:
      github:
        client-id: ${FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_ID:}
        client-secret: ${FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_SECRET:}
        callback-uri: ${FLEXBOOT4_AUTH_OAUTH_GITHUB_CALLBACK_URI:}
```

`callback-uri` 需要与 GitHub OAuth App 中配置的 Authorization callback URL 完全一致，包括协议、IP/域名、端口和路径。例如请求里出现 `http://192.168.6.7:8080/api/admin/auth/oauth/github/callback`，GitHub App 里也必须登记这一项；如果你希望统一使用 `http://localhost:8080/api/admin/auth/oauth/github/callback`，则把 `FLEXBOOT4_AUTH_OAUTH_GITHUB_CALLBACK_URI` 配成该地址并在 GitHub App 中使用同一个地址。

前端 OAuth 结果页固定为 vben 项目的 `/auth/oauth/callback`。该路径是 Admin OAuth 流程的内建约定，不作为 Spring Boot 配置项开放；后续接入其他第三方厂商也复用同一个前端结果页。

GitHub scope 固定为：

```text
read:user user:email
```

`user:email` 用于读取 GitHub 邮箱列表，系统只使用 verified email 做候选账号提示。

### 2.2 QQ 互联网站应用

在 QQ 互联创建网站应用，回调地址填写后端 OAuth callback 地址：

```text
http://localhost:8080/api/admin/auth/oauth/qq/callback
```

部署环境按实际后端公网地址填写，例如：

```text
https://api.example.com/api/admin/auth/oauth/qq/callback
```

QQ callback URL 必须指向后端接口，不是前端 `/auth/oauth/callback` 页面。

后端读取受保护配置：

```powershell
$env:FLEXBOOT4_AUTH_OAUTH_QQ_APP_ID="your-qq-app-id"
$env:FLEXBOOT4_AUTH_OAUTH_QQ_APP_KEY="your-qq-app-key"
$env:FLEXBOOT4_AUTH_OAUTH_QQ_CALLBACK_URI="http://localhost:8080/api/admin/auth/oauth/qq/callback"
```

Linux / Docker 示例：

```bash
export FLEXBOOT4_AUTH_OAUTH_QQ_APP_ID='your-qq-app-id'
export FLEXBOOT4_AUTH_OAUTH_QQ_APP_KEY='your-qq-app-key'
export FLEXBOOT4_AUTH_OAUTH_QQ_CALLBACK_URI='http://localhost:8080/api/admin/auth/oauth/qq/callback'
```

对应 Spring Boot 配置前缀为：

```yaml
flexboot4:
  auth:
    oauth:
      qq:
        app-id: ${FLEXBOOT4_AUTH_OAUTH_QQ_APP_ID:}
        app-key: ${FLEXBOOT4_AUTH_OAUTH_QQ_APP_KEY:}
        callback-uri: ${FLEXBOOT4_AUTH_OAUTH_QQ_CALLBACK_URI:}
```

QQ scope 固定为：

```text
get_user_info
```

QQ 第一版使用 OpenID 作为 `sys_user_social_account.provider_user_id`。QQ 不返回邮箱，不会按邮箱提示候选账号。

### 2.3 开启登录入口

登录页是否展示第三方入口由系统配置 `auth.login.options` 控制。示例：

```json
{
  "methods": {
    "password": {
      "enabled": true
    },
    "sms": {
      "enabled": false,
      "codeLength": 4,
      "cooldownSeconds": 60
    },
    "qrcode": {
      "enabled": false
    },
    "thirdParty": {
      "providers": [
        {
          "code": "github",
          "enabled": true
        },
        {
          "code": "qq",
          "enabled": true
        }
      ]
    },
    "register": {
      "enabled": false
    },
    "forgetPassword": {
      "enabled": true
    }
  }
}
```

第三方登录不再设置 `thirdParty.enabled` 总开关，是否展示和是否允许发起授权只由 `providers[].enabled` 控制。`providers` 只表达运行开关，展示名称、图标等由前端按 `code` 映射。GitHub `clientSecret`、QQ `appKey` 不能写入 `auth.login.options`，该配置会通过 `/api/admin/auth/options` 返回给前端。

当前初始化脚本默认保留 GitHub 与 QQ provider 配置，但不开启第三方登录：

```text
flexboot4-admin-starter/src/main/resources/db/flexboot4-migration/admin/postgresql/V1000__admin_core_schema.sql
```

已有环境如配置里仍保留 `thirdParty.enabled`，可以直接移除该字段，保留 provider 开关即可：

```sql
UPDATE sys_config
SET config_value = (config_value::jsonb #- '{methods,thirdParty,enabled}')::text
WHERE config_key = 'auth.login.options';
```

## 3. 接口流程

第三方登录入口：

```text
GET /api/admin/auth/oauth/{provider}/authorize
```

后端生成 OAuth `state`，写入 Redis 后 302 跳转到第三方授权页。当前 `provider` 支持 `github` 和 `qq`。

第三方 callback：

```text
GET /api/admin/auth/oauth/{provider}/callback
```

后端校验 `state`，使用第三方 `code` 换取 access token，然后读取第三方用户资料。第三方 access token 只在本次服务端调用中使用，不长期入库。

前端结果消费：

```text
GET /api/admin/auth/oauth/result/{ticket}
```

后端 callback 不把本地 JWT 或第三方 access token 放进 URL，只把一次性结果票据 `ticket` 带到前端 `/auth/oauth/callback`。前端消费 `ticket` 后可能得到以下状态：

| 状态 | 含义 | 前端处理 |
| --- | --- | --- |
| `LOGIN_SUCCESS` | 第三方身份已绑定启用的系统账号，且不需要 MFA | 写入本地登录态并进入系统 |
| `MFA_REQUIRED` | 第三方身份已绑定系统账号，但该账号启用了 MFA | 弹出 MFA 验证，校验通过后进入系统 |
| `BIND_REQUIRED` | 第三方身份未绑定系统账号 | 展示第三方资料、候选账号和绑定表单 |
| `ERROR` | 授权失败、配置关闭、票据过期或其他拒绝场景 | 提示重新登录 |

绑定已有系统账号：

```text
POST /api/admin/auth/oauth/{provider}/bind
```

请求体：

```json
{
  "bindTicket": "one-time-bind-ticket",
  "username": "admin",
  "password": "admin-password"
}
```

绑定成功后接口返回现有 `LoginResp`。如果该系统账号启用了 MFA，返回 MFA challenge；否则返回正式 `accessToken`。

## 4. 账号绑定规则

绑定关系保存在 `sys_user_social_account`：

| 字段 | 说明 |
| --- | --- |
| `user_id` | 本地系统用户 ID |
| `provider` | 第三方 provider，例如 `github`、`qq` |
| `provider_user_id` | 第三方平台用户唯一 ID |
| `provider_username` | 第三方平台登录名 |
| `nickname` | 第三方平台昵称 |
| `avatar_url` | 第三方头像 |
| `email` | 第三方邮箱，可能为空 |
| `email_verified` | 第三方邮箱是否已验证 |
| `bind_time` | 首次绑定时间 |
| `last_login_time` | 最近一次通过该绑定登录时间 |
| `status` | 绑定状态，`1` 为启用，`0` 为停用 |

数据库唯一性约束：

- 未删除记录中，`(provider, provider_user_id)` 唯一。
- 未删除记录中，`(user_id, provider)` 唯一。

因此，一个第三方身份只能绑定一个系统账号，一个系统账号当前对同一 provider 只能绑定一个第三方身份。若第三方身份已绑定其他系统账号，或本地账号已经绑定同 provider 的另一个身份，绑定接口会拒绝。

## 5. 个人中心解绑

当前登录用户可以查看和解绑自己的第三方绑定：

```text
GET /api/admin/user/social-accounts
DELETE /api/admin/user/social-accounts?id={socialAccountId}
```

解绑采用逻辑删除。解绑后，该第三方身份再次登录会回到 `BIND_REQUIRED` 流程，需要重新输入系统账号密码完成绑定。

## 6. 临时票据与安全控制

OAuth 过程中的临时状态全部写入 Redis，并且一次性消费：

| Key 前缀 | 默认 TTL | 说明 |
| --- | --- | --- |
| `auth:oauth:state:` | 5 分钟 | 第三方 authorize 到 callback 的 CSRF state |
| `auth:oauth:result:` | 3 分钟 | 后端 callback 跳回前端后的结果票据 |
| `auth:oauth:bind:` | 10 分钟 | 未绑定身份的绑定票据 |

安全注意事项：

- 不要把 GitHub `clientSecret` 或 QQ `appKey` 写入系统配置表、前端环境变量或前端代码。
- 不要在 URL 中传递本地 JWT 或第三方 access token。
- 生产环境必须使用 HTTPS。
- 反向代理部署时，需要正确传递 `Host`、`X-Forwarded-Proto`、`X-Forwarded-Host`，否则后端生成的 callback URL 可能与第三方应用配置不一致。
- 第三方应用的 callback URL、后端访问域名和前端实际发起登录请求的域名需要保持同一套环境，不要混用本地、测试和生产地址。

## 7. 常见问题

### 第三方按钮不显示

检查 `/api/admin/auth/options` 返回值中：

- `methods.thirdParty.providers` 是否包含对应 provider，例如 `code=github` 或 `code=qq`，且 `enabled=true`。

### 点击 GitHub 后提示未配置

检查后端环境变量：

```text
FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_ID
FLEXBOOT4_AUTH_OAUTH_GITHUB_CLIENT_SECRET
```

两项必须同时存在。修改后需要重启后端应用。

### 点击 QQ 后提示未配置

检查后端环境变量：

```text
FLEXBOOT4_AUTH_OAUTH_QQ_APP_ID
FLEXBOOT4_AUTH_OAUTH_QQ_APP_KEY
```

两项必须同时存在。修改后需要重启后端应用。

### callback 失败或 state 过期

常见原因：

- 第三方应用 callback URL 与请求中的 `redirect_uri` 或后端 `*_CALLBACK_URI` 不完全一致。
- Redis 不可用或临时 key 已过期。
- 浏览器重复打开同一个授权回调链接，第一次消费后后续会失败。
- 反向代理没有正确传递转发头，导致后端生成的 `redirect_uri` 与第三方应用中配置的不一致。

### 没有候选账号

系统只在 GitHub 返回 verified email，且本地启用用户存在同邮箱时显示候选账号。QQ 不返回邮箱，因此不会显示邮箱候选账号。没有候选账号不影响绑定，用户仍可手动输入系统账号和密码。

## 8. 扩展其他第三方厂商

后续接入其他 provider 时，保持现有接口形态不变：

1. 新增 `OAuthProviderClient` 实现，负责构造授权地址、换 token、读取第三方用户资料。
2. 新增 provider 专属受保护配置，例如 client id、client secret、scope、第三方 API 地址。
3. 在 `auth.login.options.methods.thirdParty.providers` 增加 provider 开关配置。
4. 前端 `ThirdPartyLogin` 增加 provider 文案、图标映射和点击处理。
5. 补充 provider 开关、已绑定登录、未绑定绑定、冲突拒绝、MFA 分支测试。

`sys_user_social_account` 已按 `provider + provider_user_id` 建模，通常不需要为每个第三方平台新增绑定表。
