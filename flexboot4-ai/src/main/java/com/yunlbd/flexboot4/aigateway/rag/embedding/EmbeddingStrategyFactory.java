package com.yunlbd.flexboot4.aigateway.rag.embedding;

import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.DocumentType;
import com.yunlbd.flexboot4.aigateway.rag.embedding.strategy.EmbeddingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding 策略工厂 - 管理和路由到不同的 Embedding 策略
 */
@Component
public class EmbeddingStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStrategyFactory.class);

    private final Map<DocumentType, EmbeddingStrategy> typeStrategies = new HashMap<>();
    private final Map<String, EmbeddingStrategy> modelStrategies = new HashMap<>();
    private final EmbeddingStrategy defaultStrategy;

    public EmbeddingStrategyFactory(List<EmbeddingStrategy> strategies,
                                     EmbeddingStrategy defaultStrategy) {
        this.defaultStrategy = defaultStrategy;

        // 注册所有策略
        for (EmbeddingStrategy strategy : strategies) {
            registerStrategy(strategy);
        }

        log.info("Registered {} embedding strategies", strategies.size());
    }

    /**
     * 注册策略
     */
    private void registerStrategy(EmbeddingStrategy strategy) {
        // 按模型 ID 注册
        modelStrategies.put(strategy.getModelId(), strategy);

        log.debug("Registered embedding strategy: {} -> {}", strategy.getName(), strategy.getModelId());
    }

    /**
     * 根据文档类型获取策略
     */
    public EmbeddingStrategy getStrategy(DocumentType type) {
        if (type == null) {
            return defaultStrategy;
        }

        // 查找匹配的策略
        EmbeddingStrategy strategy = typeStrategies.get(type);
        if (strategy != null) {
            return strategy;
        }

        // 返回默认策略
        return defaultStrategy;
    }

    /**
     * 根据模型 ID 获取策略
     */
    public EmbeddingStrategy getStrategyByModelId(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            return defaultStrategy;
        }

        return modelStrategies.getOrDefault(modelId, defaultStrategy);
    }

    /**
     * 获取默认策略
     */
    public EmbeddingStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    /**
     * 获取所有已注册的策略
     */
    public List<EmbeddingStrategy> getAllStrategies() {
        return List.copyOf(modelStrategies.values());
    }

    /**
     * 向量化文本（自动路由到对应策略）
     */
    public List<Float> embed(String text, DocumentType type) {
        EmbeddingStrategy strategy = getStrategy(type);
        return strategy.embed(text);
    }

    /**
     * 向量化文本（使用指定模型）
     */
    public List<Float> embed(String text, String modelId) {
        EmbeddingStrategy strategy = getStrategyByModelId(modelId);
        return strategy.embed(text);
    }

    /**
     * 批量向量化（自动路由）
     */
    public List<List<Float>> embedBatch(List<String> texts, DocumentType type) {
        EmbeddingStrategy strategy = getStrategy(type);
        return strategy.embedBatch(texts);
    }

    /**
     * 检查策略是否可用
     */
    public boolean isStrategyAvailable(DocumentType type) {
        EmbeddingStrategy strategy = getStrategy(type);
        return strategy != null && strategy.isAvailable();
    }

    /**
     * 获取策略的向量维度
     */
    public int getDimension(DocumentType type) {
        EmbeddingStrategy strategy = getStrategy(type);
        return strategy != null ? strategy.getDimension() : 0;
    }

    /**
     * 获取策略的向量维度（按模型 ID）
     */
    public int getDimension(String modelId) {
        EmbeddingStrategy strategy = getStrategyByModelId(modelId);
        return strategy != null ? strategy.getDimension() : 0;
    }
}
