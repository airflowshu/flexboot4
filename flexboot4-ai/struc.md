# flexboot4-ai 结构说明

`flexboot4-ai` 是独立的 AI Gateway 服务，负责 AI 相关接口的认证、权限、用户 API Key 配额、RAG 检索编排和 OpenAI-compatible 模型转发。当前结论是：不引入 Spring AI，不新增 `ChatProvider` 抽象，继续使用 `WebClient + LlmProxyClient` 调用上游 Chat Completions。

## 接口边界

| 场景 | 接口 | 权限码 | 行为 |
| --- | --- | --- | --- |
| 纯 AI 对话 | `/api/ai/chat`、`/api/ai/chat/stream` | `ai:chat` | 请求体按 OpenAI-compatible 形态转发到上游模型，不执行 embedding，不检索知识库 |
| 知识库问答 | `/api/ai/rag/chat`、`/api/ai/rag/chat/stream` | `kb:chat` | 对 query 做 embedding，检索知识库片段，再组装 messages 调用上游模型 |

```text
Web 前端
  ├─ 运维管理 / AI 对话
  │    └─ /api/ai/chat/stream
  │         └─ LlmProxyClient -> DeepSeek/OpenAI-compatible/APISIX
  └─ 知识库 / 知识库问答
       └─ /api/ai/rag/chat/stream
            ├─ EmbeddingHttpClient
            ├─ RagRetrievalService
            └─ LlmProxyClient -> DeepSeek/OpenAI-compatible/APISIX
```

## 上游模型配置

`llm-proxy` 是唯一的上游模型出口：

```yaml
llm-proxy:
  url: ${LLM_PROXY_URL:http://192.168.11.104:11434}
  chat-path: ${LLM_PROXY_CHAT_PATH:/v1/chat/completions}
  api-key: ${LLM_PROXY_API_KEY:}
  default-model: ${LLM_PROXY_DEFAULT_MODEL:qwen2.5:1.5b}
```

直连 DeepSeek V4 时配置：

```powershell
$env:LLM_PROXY_URL = "https://api.deepseek.com"
$env:LLM_PROXY_CHAT_PATH = "/chat/completions"
$env:LLM_PROXY_API_KEY = "sk-..."
$env:LLM_PROXY_DEFAULT_MODEL = "deepseek-v4-flash"
$env:AI_PRIMARY_MODEL = "deepseek-v4-flash"
$env:AI_FALLBACK_MODEL = "deepseek-v4-flash"
```

通过 APISIX 汇聚多厂商时，`LLM_PROXY_URL` 指向 APISIX，厂商密钥放在 APISIX 上游配置中。

## API Key 管理

- `admin-server` 负责 FlexBoot4 用户 API Key 的创建、策略和配置。
- `flexboot4-ai` 负责运行时检查、权限判断、配额扣减和审计。
- `X-AI-API-KEY` 是 FlexBoot4 内部用户 Key，可由前端传入，也可由 `flexboot4-ai` 根据登录用户从 Redis 补齐。
- `LLM_PROXY_API_KEY` 是厂商官方 Key，只能放后端配置。非空时 `LlmProxyClient` 会注入 `Authorization: Bearer ...`，并避免把前端传入的 `X-AI-API-KEY` 或 `Authorization` 透传给厂商。

## 文档

- [AI 厂商模型接入](../docs/ai-provider-models.md)
- [APISIX AI Gateway 对接](docs/apisix-ai.md)
