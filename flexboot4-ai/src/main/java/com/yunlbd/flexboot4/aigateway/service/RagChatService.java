package com.yunlbd.flexboot4.aigateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yunlbd.flexboot4.aigateway.config.RagProperties;
import com.yunlbd.flexboot4.aigateway.dto.RagChatRequest;
import com.yunlbd.flexboot4.aigateway.dto.RagRetrievedChunkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatService.class);

    private final EmbeddingHttpClient embeddingHttpClient;
    private final RagRetrievalService ragRetrievalService;
    private final LlmProxyClient llmProxyClient;
    private final ObjectMapper objectMapper;
    private final RagProperties ragProperties;

    public RagChatService(EmbeddingHttpClient embeddingHttpClient,
                          RagRetrievalService ragRetrievalService,
                          LlmProxyClient llmProxyClient,
                          ObjectMapper objectMapper,
                          RagProperties ragProperties) {
        this.embeddingHttpClient = embeddingHttpClient;
        this.ragRetrievalService = ragRetrievalService;
        this.llmProxyClient = llmProxyClient;
        this.objectMapper = objectMapper;
        this.ragProperties = ragProperties;
    }

    public Mono<JsonNode> chat(RagChatRequest request, Map<String, String> forwardHeaders) {
        return prepareChatBody(request, false)
                .doOnSubscribe(sub -> System.out.println("=== [DEBUG] RagChatService.chat subscribing"))
                .doOnSuccess(body -> System.out.println("=== [DEBUG] RagChatService.chat body prepared, model=" + body.get("model")))
                .doOnError(e -> System.out.println("=== [DEBUG] RagChatService.chat error: " + e.getClass().getName() + ": " + e.getMessage()))
                .flatMap(body -> llmProxyClient.chat(body, forwardHeaders));
    }

    public Flux<String> chatStreamData(RagChatRequest request, Map<String, String> forwardHeaders) {
        return prepareChatBody(request, true)
                .flatMapMany(body -> llmProxyClient.chatStreamRaw(body, forwardHeaders))
                .doOnNext(line -> log.debug("=== [LLM-RAW] {}", line))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    // 兼容处理：如果 WebClient 没去前缀则去掉；如果已经去掉则保持
                    if (line.startsWith("data:")) {
                        return line.substring(5).trim();
                    }
                    return line;
                })
                .filter(data -> !"[DONE]".equals(data)) // 过滤掉控制信号
                .doOnNext(data -> log.debug("=== [SSE-DATA] {}", data));
    }

    private Mono<ObjectNode> prepareChatBody(RagChatRequest request, boolean stream) {
        String query = request == null ? null : request.getQuery();
        if (query == null || query.isBlank()) {
            return Mono.error(new IllegalArgumentException("query is required"));
        }

        String embeddingModel = firstNonBlank(
                request == null ? null : request.getEmbeddingModel(),
                ragProperties.defaultEmbeddingModel(),
                "bge-m3"
        );

        int topK = firstPositive(
                request == null ? null : request.getTopK(),
                ragProperties.defaultTopK(),
                5
        );

        int maxContextChars = firstPositive(ragProperties.maxContextChars(), 8000);

        String systemPrompt = firstNonBlank(
                request == null ? null : request.getSystemPrompt(),
                ragProperties.defaultSystemPrompt(),
                "你是企业 AI 助手。请优先依据给定的知识库片段回答；若片段不足以支持结论，请明确说明不确定。"
        );

        String model = firstNonBlank(request == null ? null : request.getModel(), "default");
        String kbId = request == null ? null : request.getKbId();
        List<String> fileIds = request == null ? null : request.getFileIds();

        System.out.println("=== [DEBUG] prepareChatBody: query=" + query + ", kbId=" + kbId + ", embeddingModel=" + embeddingModel + ", topK=" + topK);

        return embeddingHttpClient.embedOne(query, embeddingModel)
                .doOnSuccess(vec -> System.out.println("=== [DEBUG] embedding completed, vector size=" + (vec == null ? 0 : vec.size())))
                .doOnError(e -> System.out.println("=== [DEBUG] embedding error: " + e.getClass().getName() + ": " + e.getMessage()))
                .flatMap(queryVector -> ragRetrievalService.retrieve(queryVector, kbId, embeddingModel, fileIds, topK).collectList())
                .doOnSuccess(chunks -> System.out.println("=== [DEBUG] retrieval completed, chunks=" + (chunks == null ? 0 : chunks.size())))
                .doOnError(e -> System.out.println("=== [DEBUG] retrieval error: " + e.getClass().getName() + ": " + e.getMessage()))
                .map(chunks -> buildChatBody(model, stream, systemPrompt, query, chunks, maxContextChars));
    }

    private ObjectNode buildChatBody(String model,
                                    boolean stream,
                                    String systemPrompt,
                                    String query,
                                    List<RagRetrievedChunkDto> chunks,
                                    int maxContextChars) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", stream);

        ArrayNode messages = root.putArray("messages");
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt));

        String context = buildContext(chunks, maxContextChars);
        if (!context.isBlank()) {
            messages.add(objectMapper.createObjectNode().put("role", "system").put("content", context));
        }

        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", query));

        log.info("=== [RAG] Built chat body: {}", root);
        return root;
    }

    private String buildContext(List<RagRetrievedChunkDto> chunks, int maxContextChars) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("以下是知识库检索到的片段（可能包含多条），仅可基于这些片段作答并在必要时引用编号：\n");

        int current = sb.length();
        int idx = 1;
        for (RagRetrievedChunkDto c : chunks) {
            if (c == null || c.getContent() == null || c.getContent().isBlank()) {
                continue;
            }
            String block = "【" + idx + "】fileId=" + safe(c.getFileId()) + ", chunkId=" + safe(c.getChunkId())
                    + ", distance=" + (c.getDistance() == null ? "" : c.getDistance())
                    + "\n" + c.getContent().trim() + "\n";

            if (current + block.length() > maxContextChars) {
                break;
            }
            sb.append(block);
            current += block.length();
            idx++;
        }

        return sb.toString();
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return null;
    }

    private static int firstPositive(Integer... candidates) {
        if (candidates == null) {
            return 0;
        }
        for (Integer c : candidates) {
            if (c != null && c > 0) {
                return c;
            }
        }
        return 0;
    }
}
