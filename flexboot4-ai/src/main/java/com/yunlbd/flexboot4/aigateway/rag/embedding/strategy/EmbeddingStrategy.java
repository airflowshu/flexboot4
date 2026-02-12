package com.yunlbd.flexboot4.aigateway.rag.embedding.strategy;

import com.yunlbd.flexboot4.aigateway.rag.embedding.EmbeddingService;

/**
 * Embedding 策略接口 - 支持按文档类型路由到不同模型
 */
public interface EmbeddingStrategy extends EmbeddingService {

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getName();

    /**
     * 判断是否支持该文档类型
     *
     * @param type 文档类型
     * @return 是否支持
     */
    boolean supports(DocumentType type);

    /**
     * 获取优先级（数值越小优先级越高）
     *
     * @return 优先级
     */
    default int getPriority() {
        return 100;
    }
}
