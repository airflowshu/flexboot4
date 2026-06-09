package com.yunlbd.flexboot4.aigateway.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

@Service
public class AiChatService {

    private final LlmProxyClient llmProxyClient;
    private final ObjectMapper objectMapper;

    public AiChatService(LlmProxyClient llmProxyClient, ObjectMapper objectMapper) {
        this.llmProxyClient = llmProxyClient;
        this.objectMapper = objectMapper;
    }

    public Mono<JsonNode> chat(JsonNode request, Map<String, String> forwardHeaders) {
        return Mono.fromSupplier(() -> prepareChatBody(request, false))
                .flatMap(body -> llmProxyClient.chat(body, forwardHeaders));
    }

    public Flux<String> chatStreamData(JsonNode request, Map<String, String> forwardHeaders) {
        return Mono.fromSupplier(() -> prepareChatBody(request, true))
                .flatMapMany(body -> llmProxyClient.chatStreamRaw(body, forwardHeaders))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    if (line.startsWith("data:")) {
                        return line.substring(5).trim();
                    }
                    return line;
                })
                .filter(data -> !"[DONE]".equals(data));
    }

    private ObjectNode prepareChatBody(JsonNode request, boolean stream) {
        ObjectNode body = normalizeRequest(request);
        if (!body.hasNonNull("model") || body.get("model").asText().isBlank()) {
            body.put("model", llmProxyClient.defaultModel());
        }
        body.put("stream", stream);
        validateMessages(body);
        return body;
    }

    private ObjectNode normalizeRequest(JsonNode request) {
        if (request == null || request.isNull()) {
            throw new IllegalArgumentException("request body is required");
        }
        if (!request.isObject()) {
            throw new IllegalArgumentException("request body must be a JSON object");
        }
        JsonNode copy = request.deepCopy();
        if (copy instanceof ObjectNode body) {
            return body;
        }
        throw new IllegalArgumentException("request body must be a JSON object");
    }

    private void validateMessages(ObjectNode body) {
        JsonNode messages = body.get("messages");
        if (messages == null || !messages.isArray() || messages.isEmpty()) {
            String query = body.path("query").asText(null);
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("messages is required");
            }
            ArrayNode normalizedMessages = objectMapper.createArrayNode();
            normalizedMessages.add(objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", query));
            body.set("messages", normalizedMessages);
            body.remove("query");
        }
    }
}
