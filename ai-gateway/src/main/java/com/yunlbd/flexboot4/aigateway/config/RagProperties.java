package com.yunlbd.flexboot4.aigateway.config;

import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.DocumentType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * RAG 配置属性
 */
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
        String defaultEmbeddingModel,
        Integer defaultTopK,
        Integer maxContextChars,
        String defaultSystemPrompt,
        Double distanceThreshold,
        StrategyConfig strategy,
        RetrievalConfig retrieval
) {

    public RagProperties() {
        this(
                "bge-m3",
                5,
                8000,
                "你是一个专业的AI助手。请根据提供的上下文信息回答用户的问题。",
                0.8,
                new StrategyConfig(null),
                new RetrievalConfig(5, 8000, 0.8)
        );
    }

    /**
     * 策略配置
     */
    public record StrategyConfig(
            List<EmbeddingStrategyConfig> mappings
    ) {
        public List<EmbeddingStrategyConfig> mappings() {
            return mappings != null ? mappings : List.of();
        }
    }

    /**
     * Embedding 策略配置
     */
    public record EmbeddingStrategyConfig(
            String type,
            String model,
            String url,
            Integer dimension
    ) {
        public DocumentType getDocumentType() {
            if (type == null || type.isEmpty()) {
                return DocumentType.TEXT;
            }
            try {
                return DocumentType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DocumentType.TEXT;
            }
        }
    }

    /**
     * 检索配置
     */
    public record RetrievalConfig(
            Integer defaultTopK,
            Integer maxContextChars,
            Double distanceThreshold
    ) {}

    /**
     * 获取指定文档类型的模型配置
     */
    public EmbeddingStrategyConfig getModelConfig(DocumentType type) {
        if (strategy == null || strategy.mappings == null) {
            return null;
        }
        return strategy.mappings.stream()
                .filter(c -> c.getDocumentType() == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取指定模型 ID 的配置
     */
    public EmbeddingStrategyConfig getModelConfig(String modelId) {
        if (strategy == null || strategy.mappings == null) {
            return null;
        }
        return strategy.mappings.stream()
                .filter(c -> modelId.equals(c.model))
                .findFirst()
                .orElse(null);
    }
}
