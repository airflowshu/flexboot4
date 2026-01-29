package com.yunlbd.flexboot4.aigateway.repository;

import com.yunlbd.flexboot4.aigateway.entity.AiVectorChunk;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 向量数据 Repository (使用 vector 数据源)
 */
@Repository
public interface VectorR2dbcRepository extends R2dbcRepository<AiVectorChunk, String> {

    /**
     * 检查指定 chunk 和模型是否已存在向量
     */
    Mono<Boolean> existsByChunkIdAndEmbeddingModel(String chunkId, String embeddingModel);
}
