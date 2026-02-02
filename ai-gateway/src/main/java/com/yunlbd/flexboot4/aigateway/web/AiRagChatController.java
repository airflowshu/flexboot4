package com.yunlbd.flexboot4.aigateway.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.yunlbd.flexboot4.aigateway.config.AiRoutingProperties;
import com.yunlbd.flexboot4.aigateway.dto.RagChatRequest;
import com.yunlbd.flexboot4.aigateway.security.AiApiKeyQuotaWebFilter;
import com.yunlbd.flexboot4.aigateway.service.AiQuotaService;
import com.yunlbd.flexboot4.aigateway.service.RagChatService;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AiRagChatController {

    private final RagChatService ragChatService;
    private final AiRoutingProperties routingProperties;
    private final AiQuotaService quotaService;

    public AiRagChatController(RagChatService ragChatService, AiRoutingProperties routingProperties, AiQuotaService quotaService) {
        this.ragChatService = ragChatService;
        this.routingProperties = routingProperties;
        this.quotaService = quotaService;
    }

    @PostMapping("/api/ai/rag/chat")
    @OperLog(title = "AI RAG Chat", isSaveRequestData = true, isSaveResponseData = false)
    public Mono<ApiResult<JsonNode>> chat(@RequestBody RagChatRequest request, ServerWebExchange exchange) {
        ApiKeyRule rule = (ApiKeyRule) exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_API_KEY_RULE);
        boolean exhausted = Boolean.TRUE.equals(exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_QUOTA_EXHAUSTED));

        applyModelRouting(request, exhausted);

        return ragChatService.chat(request, buildForwardHeaders(exchange))
                .flatMap(resp -> {
                    if (exhausted || rule == null) {
                        return Mono.just(ApiResult.success(resp));
                    }
                    long totalTokens = readTotalTokens(resp);
                    return quotaService.addUsage(rule, totalTokens)
                            .thenReturn(ApiResult.success(resp));
                });
    }

    @PostMapping(value = "/api/ai/rag/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @OperLog(title = "AI RAG 对话")
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody RagChatRequest request, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().set("Cache-Control", "no-cache, no-transform");
        exchange.getResponse().getHeaders().set("X-Accel-Buffering", "no");
        exchange.getResponse().getHeaders().set("Connection", "keep-alive");
        exchange.getResponse().getHeaders().set("Pragma", "no-cache");
        boolean exhausted = Boolean.TRUE.equals(exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_QUOTA_EXHAUSTED));
        applyModelRouting(request, exhausted);

        return ragChatService.chatStreamData(request, buildForwardHeaders(exchange))
                .map(data -> ServerSentEvent.builder(data).build());
    }

    //转发上游时优先从 exchange attribute 取回 apiKey，并注入到转发头 X-AI-API-KEY （即使前端没传也能发给 APISIX）
    private Map<String, String> buildForwardHeaders(ServerWebExchange exchange) {
        Map<String, String> headers = new HashMap<>();
        String aiApiKey = exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_API_KEY_RAW);
        if (aiApiKey == null || aiApiKey.isBlank()) {
            aiApiKey = exchange.getRequest().getHeaders().getFirst("X-AI-API-KEY");
        }
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            headers.put("X-AI-API-KEY", aiApiKey);
        }
        return headers;
    }

    private void applyModelRouting(RagChatRequest request, boolean exhausted) {
        if (request == null) {
            return;
        }
        if (exhausted) {
            String fallback = routingProperties.fallbackModel();
            if (fallback != null && !fallback.isBlank()) {
                request.setModel(fallback);
            }
            return;
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            String primary = routingProperties.primaryModel();
            if (primary != null && !primary.isBlank()) {
                request.setModel(primary);
            }
        }
    }

    private long readTotalTokens(JsonNode resp) {
        if (resp == null) {
            return 0L;
        }
        JsonNode usage = resp.get("usage");
        if (usage == null) {
            return 0L;
        }
        JsonNode total = usage.get("total_tokens");
        if (total == null || !total.isNumber()) {
            return 0L;
        }
        return total.asLong(0L);
    }
}
