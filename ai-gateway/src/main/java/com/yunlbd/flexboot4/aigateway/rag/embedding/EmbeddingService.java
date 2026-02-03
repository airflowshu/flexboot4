package com.yunlbd.flexboot4.aigateway.rag.embedding;

import java.util.List;

/**
 * Embedding 服务接口 - 定义向量化操作的抽象
 */
public interface EmbeddingService {

    /**
     * 向量化单个文本
     *
     * @param text 输入文本
     * @return 向量
     */
    List<Float> embed(String text);

    /**
     * 批量向量化文本
     *
     * @param texts 输入文本列表
     * @return 向量列表
     */
    List<List<Float>> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     *
     * @return 维度数
     */
    int getDimension();

    /**
     * 获取模型标识符
     *
     * @return 模型 ID
     */
    String getModelId();

    /**
     * 检查服务是否可用
     *
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}
