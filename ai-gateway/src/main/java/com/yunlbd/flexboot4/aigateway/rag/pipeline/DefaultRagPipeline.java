package com.yunlbd.flexboot4.aigateway.rag.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yunlbd.flexboot4.aigateway.config.RagProperties;
import com.yunlbd.flexboot4.aigateway.dto.RagChatRequest;
import com.yunlbd.flexboot4.aigateway.dto.RagRetrievedChunkDto;
import com.yunlbd.flexboot4.aigateway.rag.embedding.EmbeddingStrategyFactory;
import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.DocumentType;
import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.EmbeddingStrategy;
import com.yunlbd.flexboot4.aigateway.rag.vectorstore.VectorStore;
import com.yunlbd.flexboot4.aigateway.service.LlmProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认 RAG 流程实现
 */
@Service
public class DefaultRagPipeline implements RagPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultRagPipeline.class);

    private final EmbeddingStrategyFactory embeddingFactory;
    private final VectorStore vectorStore;
    private final LlmProxyClient llmClient;
    private final RagProperties properties;
    private final ObjectMapper objectMapper;

    public DefaultRagPipeline(EmbeddingStrategyFactory embeddingFactory,
                              VectorStore vectorStore,
                              LlmProxyClient llmClient,
                              RagProperties properties,
                              ObjectMapper objectMapper) {
        this.embeddingFactory = embeddingFactory;
        this.vectorStore = vectorStore;
        this.llmClient = llmClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<JsonNode> chat(RagChatRequest request, Map<String, String> forwardHeaders) {
        // 1. 解析文档类型和模型
        DocumentType docType = resolveDocumentType(request);
        String embeddingModel = resolveEmbeddingModel(request, docType);
        int topK = firstPositive(
                request.getTopK(),
                properties.retrieval() != null ? properties.retrieval().defaultTopK() : null,
                properties.defaultTopK()
        );

        // 2. 向量化查询
        return Mono.fromCallable(() -> {
            EmbeddingStrategy strategy = embeddingFactory.getStrategyByModelId(embeddingModel);
            return strategy.embed(request.getQuery());
        }).flatMap(queryVector -> {
            // 3. 检索相似文档
            return Mono.fromCallable(() ->
                    vectorStore.search(queryVector, embeddingModel, request.getFileIds(), topK)
            ).flatMap(hits -> {
                // 4. 构建 Prompt
                List<RagRetrievedChunkDto> chunks = hits.stream()
                        .map(hit -> new RagRetrievedChunkDto(
                                hit.chunkId(),
                                hit.fileId(),
                                null, // chunkIndex
                                null, // content - 需要从 admin 库获取
                                hit.tokens(),
                                hit.distance()
                        ))
                        .sorted(Comparator.comparingDouble(RagRetrievedChunkDto::getDistance).reversed())
                        .collect(Collectors.toList());

                return Mono.just(buildChatBody(request, chunks));
            });
        }).flatMap(body -> llmClient.chat(body, forwardHeaders));
    }

    @Override
    public Flux<String> chatStream(RagChatRequest request, Map<String, String> forwardHeaders) {
        // TODO: 实现流式版本
        return Flux.error(new UnsupportedOperationException("Stream not implemented yet"));
    }

    @Override
    public Mono<List<RagRetrievedChunkDto>> retrieve(RagChatRequest request) {
        DocumentType docType = resolveDocumentType(request);
        String embeddingModel = resolveEmbeddingModel(request, docType);
        int topK = firstPositive(
                request.getTopK(),
                properties.retrieval() != null ? properties.retrieval().defaultTopK() : null,
                properties.defaultTopK()
        );

        return Mono.fromCallable(() -> {
            EmbeddingStrategy strategy = embeddingFactory.getStrategyByModelId(embeddingModel);
            List<Float> queryVector = strategy.embed(request.getQuery());

            return vectorStore.search(queryVector, embeddingModel, request.getFileIds(), topK);
        }).map(hits -> hits.stream()
                .map(hit -> new RagRetrievedChunkDto(
                        hit.chunkId(),
                        hit.fileId(),
                        null,
                        null,
                        hit.tokens(),
                        hit.distance()
                ))
                .sorted(Comparator.comparingDouble(RagRetrievedChunkDto::getDistance).reversed())
                .collect(Collectors.toList()));
    }

    @Override
    public Mono<Boolean> embed(String content, String chunkId, String fileId, String model) {
        return Mono.fromCallable(() -> {
            EmbeddingStrategy strategy = embeddingFactory.getStrategyByModelId(model);
            List<Float> vector = strategy.embed(content);

            // 估算 token 数（简单估算：中文约 1.5 chars/token，英文约 4 chars/token）
            int tokens = estimateTokens(content);

            vectorStore.save(chunkId, fileId, model, vector, tokens);
            return true;
        });
    }

    /**
     * 解析文档类型
     */
    private DocumentType resolveDocumentType(RagChatRequest request) {
        if (request.getFileIds() != null && !request.getFileIds().isEmpty()) {
            String fileId = request.getFileIds().get(0);
            return DocumentType.fromFileId(fileId);
        }
        return DocumentType.TEXT;
    }

    /**
     * 解析 embedding 模型
     */
    private String resolveEmbeddingModel(RagChatRequest request, DocumentType docType) {
        // 1. 优先使用请求中指定的模型
        if (firstNonBlank(request.getEmbeddingModel()) != null) {
            return request.getEmbeddingModel();
        }

        // 2. 从配置中获取该文档类型的默认模型
        if (properties.getModelConfig(docType) != null) {
            return properties.getModelConfig(docType).model();
        }

        // 3. 使用全局默认模型
        return firstNonBlank(properties.defaultEmbeddingModel(), "bge-m3");
    }

    /**
     * 构建 LLM 请求体
     */
    private ObjectNode buildChatBody(RagChatRequest request, List<RagRetrievedChunkDto> chunks) {
        ObjectNode body = objectMapper.createObjectNode();

        String model = firstNonBlank(request.getModel(), "qwen2.5:7b");
        body.put("model", model);

        int maxContextChars = properties.retrieval() != null
                ? properties.retrieval().maxContextChars()
                : properties.maxContextChars();

        // 构建上下文
        String context = buildContext(chunks, maxContextChars);

        // 构建消息
        ArrayNode messages = body.putArray("messages");

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", properties.defaultSystemPrompt());
        messages.add(systemMsg);

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", """
                基于以下上下文信息回答问题。如果上下文中没有相关信息，请说明你不知道。

                上下文：
                %s

                问题：%s
                """.formatted(context, request.getQuery()));
        messages.add(userMsg);

        // 流式标记
        body.put("stream", false);

        return body;
    }

    /**
     * 构建上下文字符串
     */
    private String buildContext(List<RagRetrievedChunkDto> chunks, int maxContextChars) {
        if (chunks.isEmpty()) {
            return "未找到相关内容";
        }

        StringBuilder context = new StringBuilder();
        double distanceThreshold = properties.retrieval() != null
                ? properties.retrieval().distanceThreshold()
                : properties.distanceThreshold();

        List<String> usedChunkIds = new ArrayList<>();
        int index = 1;

        for (RagRetrievedChunkDto chunk : chunks) {
            // 跳过距离过大的结果
            if (chunk.getDistance() != null && chunk.getDistance() > distanceThreshold) {
                continue;
            }

            // 跳过重复的分块
            if (usedChunkIds.contains(chunk.getChunkId())) {
                continue;
            }
            usedChunkIds.add(chunk.getChunkId());

            // 添加编号引用
            context.append("【%d】fileId=%s, chunkId=%s, distance=%s\n".formatted(
                    index, chunk.getFileId(), chunk.getChunkId(),
                    chunk.getDistance() != null ? String.format("%.4f", chunk.getDistance()) : "N/A"));

            index++;

            // 如果超出限制，截取前面的部分
            if (context.length() > maxContextChars) {
                return context.substring(0, maxContextChars) + "...";
            }
        }

        return context.toString();
    }

    /**
     * 估算 token 数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：平均 4 个字符一个 token
        return text.length() / 4;
    }

    // ===== 辅助方法 =====

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
