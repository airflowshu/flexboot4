package com.yunlbd.flexboot4.aigateway.repository.impl;

import com.yunlbd.flexboot4.aigateway.dto.FileChunkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 文件分片 Repository 实现 (使用 admin 数据源，只读)
 */
@Repository
public class FileChunkRepositoryImpl {

    private static final Logger log = LoggerFactory.getLogger(FileChunkRepositoryImpl.class);

    private final R2dbcEntityTemplate adminTemplate;

    public FileChunkRepositoryImpl(@Qualifier("adminR2dbcEntityTemplate") R2dbcEntityTemplate adminTemplate) {
        this.adminTemplate = adminTemplate;
    }

    /**
     * 根据 chunk ID 查询分片信息
     */
    public Mono<FileChunkDto> findById(String chunkId) {
        String sql = """
            SELECT id, file_id, chunk_index, content, content_hash, embedding_model, token_count, embed_status
            FROM sys_file_chunk WHERE id = $1
            """;

        return adminTemplate.getDatabaseClient()
                .sql(sql)
                .bind(0, chunkId)
                .map((row, metadata) -> new FileChunkDto(
                        row.get("id", String.class),
                        row.get("file_id", String.class),
                        row.get("chunk_index", Integer.class),
                        row.get("content", String.class),
                        row.get("content_hash", String.class),
                        row.get("embedding_model", String.class),
                        row.get("token_count", Integer.class),
                        row.get("embed_status", String.class)
                ))
                .one();
    }
}
