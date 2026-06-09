# AI 厂商模型接入指南

本文说明 `flexboot4-ai` 如何接入 DeepSeek V4、OpenAI-compatible 云厂商模型或内部模型网关。当前方案保持直接、可控：

- 不引入 Spring AI。
- 不新增 `ChatProvider` 适配层。
- 由 `LlmProxyClient` 使用 `WebClient` 转发 OpenAI-compatible Chat Completions 请求。
- 厂商 API Key 只配置在后端环境变量中，前端不得持有。

## 1. 接入边界

`flexboot4-ai` 当前拆成两个明确入口：

| 使用场景 | 前端页面 | 后端接口 | 上游调用 |
| --- | --- | --- | --- |
| AI 对话 | `运维管理 / AI 对话` | `POST /api/ai/chat`、`POST /api/ai/chat/stream` | 直接调用第三方或官方模型，不做 embedding 和知识库检索 |
| 知识库问答 | `知识库 / 知识库问答` | `POST /api/ai/rag/chat`、`POST /api/ai/rag/chat/stream` | 先 embedding + 向量检索，再把知识库片段拼入 Chat Completions |

因此，接入 DeepSeek、OpenAI-compatible 厂商或 APISIX 时，只需要调整 `llm-proxy` 指向。纯 AI 对话和 RAG 问答共用同一个上游模型出口，但业务流程不同。

## 2. 配置项

`flexboot4-ai/src/main/resources/application.yml` 已支持以下环境变量：

| 环境变量 | 说明 | DeepSeek V4 示例 |
| --- | --- | --- |
| `LLM_PROXY_URL` | OpenAI-compatible base URL | `https://api.deepseek.com` |
| `LLM_PROXY_CHAT_PATH` | Chat Completions 路径 | `/chat/completions` |
| `LLM_PROXY_API_KEY` | 厂商官方 API Key。非空时后端转发会注入 `Authorization: Bearer ...` | `sk-...` |
| `LLM_PROXY_DEFAULT_MODEL` | 纯 AI 对话未传 `model` 时使用的默认模型 | `deepseek-v4-flash` |
| `AI_PRIMARY_MODEL` | RAG 问答未传 `model` 时使用的主模型 | `deepseek-v4-flash` |
| `AI_FALLBACK_MODEL` | RAG 问答配额耗尽或路由兜底时使用的模型 | `deepseek-v4-flash` |

注意区分两类 Key：

- `LLM_PROXY_API_KEY` 是 DeepSeek、OpenAI 等厂商密钥，只能放在后端。
- `X-AI-API-KEY` 是 FlexBoot4 自己的用户 API Key，用于用户授权、配额和审计。直连厂商官方 API 且配置了 `LLM_PROXY_API_KEY` 时，`LlmProxyClient` 不会把 `X-AI-API-KEY` 或前端传入的 `Authorization` 透传给厂商。

## 3. 直连 DeepSeek V4

截至 2026-06-09，DeepSeek 官方 API 文档给出的 OpenAI 兼容参数为：

| 参数 | 值 |
| --- | --- |
| base URL | `https://api.deepseek.com` |
| Chat Completions | `/chat/completions` |
| V4 Flash | `deepseek-v4-flash` |
| V4 Pro | `deepseek-v4-pro` |

`deepseek-v4-flash` 适合作为默认模型；`deepseek-v4-pro` 适合更复杂的推理、编码和高质量问答。官方文档同时说明旧模型名 `deepseek-chat` 与 `deepseek-reasoner` 将在 2026-07-24 15:59 UTC 停用，因此新配置应直接使用 V4 模型名。

Windows PowerShell：

```powershell
$env:LLM_PROXY_URL = "https://api.deepseek.com"
$env:LLM_PROXY_CHAT_PATH = "/chat/completions"
$env:LLM_PROXY_API_KEY = "sk-..."
$env:LLM_PROXY_DEFAULT_MODEL = "deepseek-v4-flash"
$env:AI_PRIMARY_MODEL = "deepseek-v4-flash"
$env:AI_FALLBACK_MODEL = "deepseek-v4-flash"

.\gradlew.bat :flexboot4-ai:bootRun
```

Linux/macOS：

```bash
export LLM_PROXY_URL=https://api.deepseek.com
export LLM_PROXY_CHAT_PATH=/chat/completions
export LLM_PROXY_API_KEY=sk-...
export LLM_PROXY_DEFAULT_MODEL=deepseek-v4-flash
export AI_PRIMARY_MODEL=deepseek-v4-flash
export AI_FALLBACK_MODEL=deepseek-v4-flash

./gradlew :flexboot4-ai:bootRun
```

如果需要在纯 AI 对话中显式使用 V4 Pro 或开启 DeepSeek 特定参数，可以在请求体里传 OpenAI-compatible 字段。`/api/ai/chat` 会保留额外字段并原样转发：

```json
{
  "model": "deepseek-v4-pro",
  "messages": [
    { "role": "system", "content": "你是企业 AI 助手。" },
    { "role": "user", "content": "请解释 FlexBoot4 的模块边界。" }
  ],
  "thinking": { "type": "enabled" },
  "reasoning_effort": "high"
}
```

RAG 问答接口当前只接收 `RagChatRequest` 中定义的字段，不透传任意厂商私有参数。需要控制 DeepSeek thinking mode 时，优先使用模型默认行为；如果业务确实需要在知识库问答里暴露这些参数，应明确扩展 `RagChatRequest` 和 RAG 组装逻辑。

## 4. 接口验证

启动 `flexboot4-ai` 后，可以先验证纯 AI 对话。下面示例需要替换为当前登录用户的 `access_token` 和 FlexBoot4 内部 `X-AI-API-KEY`：

```bash
curl -N "http://localhost:8081/api/ai/chat/stream" \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<登录后的 JWT>" \
  -H "X-AI-API-KEY: <FlexBoot4 用户 API Key>" \
  -d '{
    "model": "deepseek-v4-flash",
    "messages": [
      { "role": "user", "content": "你好，请用一句话介绍 FlexBoot4。" }
    ]
  }'
```

知识库问答需要已有知识库和向量数据，并且请求体必须带 `kbId`：

```bash
curl -N "http://localhost:8081/api/ai/rag/chat/stream" \
  -H "Content-Type: application/json" \
  -H "Cookie: access_token=<登录后的 JWT>" \
  -H "X-AI-API-KEY: <FlexBoot4 用户 API Key>" \
  -d '{
    "kbId": "<知识库 ID>",
    "query": "请根据知识库说明项目如何接入 DeepSeek V4。",
    "model": "deepseek-v4-flash",
    "topK": 5
  }'
```

## 5. 通过 APISIX 汇聚多厂商

如果需要统一管理多个模型、做限流、熔断、fallback 或观测，推荐让 `flexboot4-ai` 指向 APISIX：

```powershell
$env:LLM_PROXY_URL = "http://127.0.0.1:9080"
$env:LLM_PROXY_CHAT_PATH = "/v1/chat/completions"
$env:LLM_PROXY_API_KEY = ""
$env:LLM_PROXY_DEFAULT_MODEL = "deepseek-v4-flash"
```

这种模式下：

- `flexboot4-ai` 仍负责 JWT、用户 API Key、权限、配额和业务审计。
- APISIX 负责上游模型实例、厂商密钥、路由、限流和 fallback。
- `X-AI-API-KEY` 可以作为 APISIX consumer key 使用。
- DeepSeek 官方密钥应配置在 APISIX 的上游插件或服务配置中，而不是前端。

APISIX 示例见 [APISIX AI Gateway 对接](https://github.com/airflowshu/flexboot4/blob/master/flexboot4-ai/docs/apisix-ai.md)。

## 6. 接入其它 OpenAI-compatible 厂商

接入其它厂商时按同一流程处理：

1. 确认厂商是否支持 Chat Completions 兼容接口。
2. 设置 `LLM_PROXY_URL` 为厂商 base URL。
3. 设置 `LLM_PROXY_CHAT_PATH` 为厂商 Chat Completions 路径，例如 `/v1/chat/completions` 或 `/chat/completions`。
4. 设置 `LLM_PROXY_API_KEY`，让后端统一注入 `Authorization: Bearer ...`。
5. 设置 `LLM_PROXY_DEFAULT_MODEL`、`AI_PRIMARY_MODEL`、`AI_FALLBACK_MODEL`。
6. 用 `/api/ai/chat/stream` 验证纯 AI 对话，再用 `/api/ai/rag/chat/stream` 验证知识库问答。

如果厂商不是 OpenAI-compatible Chat Completions 形态，再考虑单独适配；当前不为未发生的差异提前引入 Spring AI 或 Provider 抽象。

## 7. 常见问题

### 返回 401 或 403

先确认请求携带了登录态和 FlexBoot4 内部 API Key。`LLM_PROXY_API_KEY` 只能解决上游厂商鉴权，不能替代 FlexBoot4 的用户授权。

### DeepSeek 返回 401

通常是 `LLM_PROXY_API_KEY` 无效、未充值、环境变量未生效，或 APISIX 上游插件没有正确注入 `Authorization`。

### DeepSeek 返回 404

优先检查 `LLM_PROXY_CHAT_PATH`。直连 DeepSeek V4 推荐使用 `/chat/completions`；如果使用 `/v1/chat/completions`，需要确认当前官方接口是否仍兼容。

### 模型不存在

确认 `model` 为 `deepseek-v4-flash` 或 `deepseek-v4-pro`。不要在新配置中继续使用即将停用的 `deepseek-chat`、`deepseek-reasoner`。

### AI 对话能用，知识库问答不能用

RAG 还依赖 embedding 服务、向量库、知识库文件切片和 `kbId`。DeepSeek V4 只负责最终回答，不替代 embedding 和检索链路。

## 8. 官方参考

- [DeepSeek API: Your First API Call](https://api-docs.deepseek.com/)
- [DeepSeek API: Models & Pricing](https://api-docs.deepseek.com/quick_start/pricing)
- [DeepSeek V4 Preview Release](https://api-docs.deepseek.com/news/news260424)
