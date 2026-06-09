package com.yunlbd.flexboot4.aigateway.web;

import com.yunlbd.flexboot4.aigateway.security.AiApiKeyQuotaWebFilter;
import com.yunlbd.flexboot4.aigateway.service.AiChatService;
import com.yunlbd.flexboot4.aigateway.service.AiQuotaService;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiQuotaService quotaService;

    public AiChatController(AiChatService aiChatService, AiQuotaService quotaService) {
        this.aiChatService = aiChatService;
        this.quotaService = quotaService;
    }

    @RequirePermission("ai:chat")
    @PostMapping("/api/ai/chat")
    @OperLog(title = "AI 对话", businessType = BusinessType.API, isSaveRequestData = true, isSaveResponseData = false)
    public Mono<ApiResult<JsonNode>> chat(@RequestBody JsonNode request, ServerWebExchange exchange) {
        ApiKeyRule rule = (ApiKeyRule) exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_API_KEY_RULE);
        boolean exhausted = Boolean.TRUE.equals(exchange.getAttribute(AiApiKeyQuotaWebFilter.ATTR_QUOTA_EXHAUSTED));
        return aiChatService.chat(request, buildForwardHeaders(exchange))
                .flatMap(resp -> {
                    if (exhausted || rule == null) {
                        return Mono.just(ApiResult.success(resp));
                    }
                    return quotaService.addUsage(rule, readTotalTokens(resp))
                            .thenReturn(ApiResult.success(resp));
                });
    }

    @RequirePermission("ai:chat")
    @PostMapping(value = "/api/ai/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @OperLog(title = "AI 流式对话", businessType = BusinessType.API)
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody JsonNode request, ServerWebExchange exchange) {
        exchange.getResponse().getHeaders().set("Cache-Control", "no-cache, no-transform");
        exchange.getResponse().getHeaders().set("X-Accel-Buffering", "no");
        exchange.getResponse().getHeaders().set("Connection", "keep-alive");
        exchange.getResponse().getHeaders().set("Pragma", "no-cache");

        return aiChatService.chatStreamData(request, buildForwardHeaders(exchange))
                .map(data -> ServerSentEvent.builder(data).build());
    }

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
