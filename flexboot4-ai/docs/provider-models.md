# AI 厂商模型接入

`flexboot4-ai` 的厂商模型接入说明已收口到项目文档站，详见：

- [AI 厂商模型接入指南](../../docs/ai-provider-models.md)
- [APISIX AI Gateway 对接](apisix-ai.md)

核心结论：

- 不引入 Spring AI。
- 不新增 `ChatProvider` 适配层。
- 继续使用 `WebClient + LlmProxyClient` 转发 OpenAI-compatible Chat Completions。
- DeepSeek、OpenAI-compatible 官方 API Key 只放后端配置，不放前端。
