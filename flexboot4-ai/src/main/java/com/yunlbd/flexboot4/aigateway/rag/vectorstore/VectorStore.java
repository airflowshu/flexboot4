package com.yunlbd.flexboot4.aigateway.rag.vectorstore;

import java.util.List;

/**
 * 向量存储接口 - 定义向量操作的抽象
 */
public interface VectorStore {

    /**
     * 保存单个向量
     *
     * @param chunkId      分块 ID
     * @param fileId       文件 ID
     * @param model        embedding 模型
     * @param vector       向量
     * @param tokens       token 数
     */
    void save(String chunkId, String fileId, String model, List<Float> vector, Integer tokens);

    /**
     * 批量保存向量
     *
     * @param documents 向量文档列表
     */
    void saveBatch(List<VectorDocument> documents);

    /**
     * 搜索相似向量
     *
     * @param queryVector 查询向量
     * @param model       embedding 模型
     * @param fileIds     文件 ID 过滤（null 表示不过滤）
     * @param topK        返回数量
     * @return 搜索结果
     */
    List<VectorSearchHit> search(List<Float> queryVector, String model, List<String> fileIds, int topK);

    /**
     * 根据文件 ID 删除所有向量
     *
     * @param fileId 文件 ID
     */
    void deleteByFileId(String fileId);

    /**
     * 根据分块 ID 删除向量
     *
     * @param chunkIds 分块 ID 列表
     */
    void deleteByChunkIds(List<String> chunkIds);

    /**
     * 检查向量是否存在
     *
     * @param chunkId 分块 ID
     * @param model   embedding 模型
     * @return 是否存在
     */
    boolean exists(String chunkId, String model);

    /**
     * 向量文档
     */
    record VectorDocument(
            String chunkId,
            String fileId,
            String model,
            List<Float> vector,
            Integer tokens,
            String content
    ) {}

    /**
     * 向量搜索命中结果
     */
    record VectorSearchHit(
            String chunkId,
            String fileId,
            String content,
            Double distance,
            Integer tokens
    ) {}
}
