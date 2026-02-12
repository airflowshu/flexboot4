# APISIX AI Gateway 对接（ai-gateway → APISIX → 多模型）

ai-gateway 已支持把生成请求转发到上游（OpenAI-compatible），并在配额耗尽时切到 fallback model。推荐把上游统一指向 APISIX，从而实现多模型管理、热切换、限流与兜底。

## 1. ai-gateway 侧配置

`application.yml`（或环境变量）：

```yaml
llm-proxy:
  url: http://127.0.0.1:9080
  chat-path: /v1/chat/completions
```

请求时携带：
- Cookie: access_token=...
- X-AI-API-KEY: <用户的 aiApiKey>（可选：前端不传时由 ai-gateway 按 userId 从 Redis 自动补齐，并在转发 APISIX 时注入）

## 2. APISIX 侧建议路由（OpenAI-compatible 入口）

### 2.1 Key Auth（可选，但推荐）

让 APISIX 使用 `X-AI-API-KEY` 作为 consumer key：
- route/service 启用 `key-auth`
- key_names 配置为 `["X-AI-API-KEY"]`
- consumer 的 credential key 设置为用户的 aiApiKey（与 admin-server 发放一致）

### 2.2 多模型 + 兜底（ai-proxy-multi + ai-rate-limiting）

思路：
- 一个统一入口 `/v1/chat/completions`
- instances[0]：云模型（主）
- instances[1]：Ollama 本地 qwen（兜底）
- 当主实例触发 token-based rate limiting 时，自动 fallback 到兜底实例

示例（需要按你的 APISIX 版本与 admin key 调整字段）：

```bash
curl "http://127.0.0.1:9180/apisix/admin/routes/ai-chat" -X PUT \
  -H "X-API-KEY: ${ADMIN_API_KEY}" \
  -d '{
    "uri": "/v1/chat/completions",
    "methods": ["POST"],
    "plugins": {
      "key-auth": { "key_names": ["X-AI-API-KEY"] },
      "ai-proxy-multi": {
        "fallback_strategy": ["rate_limiting"],
        "instances": [
          {
            "name": "cloud-primary",
            "provider": "openai",
            "priority": 1,
            "weight": 0,
            "auth": { "header": { "Authorization": "Bearer ${CLOUD_API_KEY}" } },
            "options": { "model": "gpt-4o-mini" }
          },
          {
            "name": "ollama-fallback",
            "provider": "openai",
            "priority": 2,
            "weight": 0,
            "auth": { "header": { } },
            "options": {
              "model": "qwen2.5",
              "endpoint": "http://127.0.0.1:11434/v1"
            }
          }
        ]
      },
      "ai-rate-limiting": {
        "policy": "redis",
        "limit_strategy": "total_tokens",
        "instances": [
          { "name": "cloud-primary", "limit": 100000, "time_window": 86400 }
        ]
      }
    }
  }'
```

说明：
- `ai-rate-limiting` 的 time_window 可以与你的“重置周期”对齐（如 86400 秒=日）。
- 兜底是否计费，取决于你的业务规则：可在 ai-gateway 侧决定是否记账（当前实现：配额耗尽时不记账）。

## 3. 实战建议

- 业务配额（按用户/Key/周期）优先在 ai-gateway 控制：更贴合你的管理后台规则与计费口径。\n+- 基础防护与流量治理在 APISIX 做：实例级 token 限流、重试、熔断、fallback、观测指标等。
