package com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.impl;

import com.yunlbd.flexboot4.aigateway.config.EmbeddingHttpProperties;
import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.DocumentType;
import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.EmbeddingStrategy;
import com.yunlbd.flexboot4.aigateway.service.EmbeddingHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 默认 Embedding 策略 - 基于现有的 EmbeddingHttpClient
 * 支持所有文档类型，使用统一的 Ollama Embedding 服务
 */
@Component
public class DefaultEmbeddingStrategy implements EmbeddingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmbeddingStrategy.class);

    private final EmbeddingHttpClient embeddingHttpClient;
    private final String model;
    private final int dimension;

    @Autowired
    public DefaultEmbeddingStrategy(EmbeddingHttpClient embeddingHttpClient,
                                     EmbeddingHttpProperties properties) {
        this.embeddingHttpClient = embeddingHttpClient;
        // 默认使用 bge-m3 模型
        this.model = "bge-m3";
        this.dimension = 1024;
    }

    public DefaultEmbeddingStrategy(EmbeddingHttpClient embeddingHttpClient,
                                     String model,
                                     int dimension) {
        this.embeddingHttpClient = embeddingHttpClient;
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public String getModelId() {
        return model;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public boolean supports(DocumentType type) {
        // 默认策略支持所有类型
        return true;
    }

    @Override
    public int getPriority() {
        // 默认策略优先级最低
        return Integer.MAX_VALUE;
    }

    @Override
    public List<Float> embed(String text) {
        // 同步调用（阻塞）
        Mono<List<Float>> mono = embeddingHttpClient.embedOne(text, model);
        return mono.block();
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        // 批量调用（阻塞）
        Mono<List<List<Float>>> mono = embeddingHttpClient.embed(texts, model);
        List<List<Float>> result = mono.block();
        return result != null ? result : List.of();
    }

    @Override
    public boolean isAvailable() {
        try {
            List<Float> test = embed("test");
            return test != null && !test.isEmpty();
        } catch (Exception e) {
            log.warn("Embedding service unavailable: {}", e.getMessage());
            return false;
        }
    }
}
