package com.yunlbd.flexboot4.aigateway.rag.vectorstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.StringJoiner;

import static com.yunlbd.flexboot4.aigateway.util.PgVectorUtils.toPgVectorString;

/**
 * PgVector 向量存储实现
 * 基于现有的 VectorSearchService 和 VectorWriteService
 */
@Component
public class PgVectorStoreImpl implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreImpl.class);

    private final R2dbcEntityTemplate vectorTemplate;

    public PgVectorStoreImpl(@Qualifier("vectorR2dbcEntityTemplate") R2dbcEntityTemplate vectorTemplate) {
        this.vectorTemplate = vectorTemplate;
    }

    @Override
    public void save(String chunkId, String fileId, String model, List<Float> vector, Integer tokens) {
        String vectorStr = toPgVectorString(vector);

        String sql = """
            INSERT INTO ai_vector_chunk (chunk_id, file_id, embedding_model, vector, tokens, created_at)
            VALUES ($1, $2, $3, $4::vector, $5, $6)
            ON CONFLICT (chunk_id, embedding_model) DO UPDATE SET
                vector = EXCLUDED.vector,
                tokens = EXCLUDED.tokens,
                created_at = EXCLUDED.created_at
            """;

        vectorTemplate.getDatabaseClient()
                .sql(sql)
                .bind(0, chunkId)
                .bind(1, fileId)
                .bind(2, model)
                .bind(3, vectorStr)
                .bind(4, tokens)
                .bind(5, LocalDateTime.now())
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Override
    public void saveBatch(List<VectorDocument> documents) {
        for (VectorDocument doc : documents) {
            save(doc.chunkId(), doc.fileId(), doc.model(), doc.vector(), doc.tokens());
        }
    }

    @Override
    public List<VectorSearchHit> search(List<Float> queryVector, String model, List<String> fileIds, int topK) {
        if (topK <= 0) {
            return List.of();
        }

        String vectorStr = toPgVectorString(queryVector);
        StringBuilder sql = new StringBuilder("""
            SELECT chunk_id, file_id, tokens, (vector <-> $1::vector) AS distance
            FROM ai_vector_chunk
            WHERE embedding_model = $2
            """);

        int nextPlaceholder = 3;
        if (fileIds != null && !fileIds.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (int i = 0; i < fileIds.size(); i++) {
                joiner.add("$" + nextPlaceholder);
                nextPlaceholder++;
            }
            sql.append(" AND file_id IN (").append(joiner).append(")\n");
        }

        sql.append(" ORDER BY vector <-> $1::vector\n");
        sql.append(" LIMIT $").append(nextPlaceholder);

        var spec = vectorTemplate.getDatabaseClient()
                .sql(sql.toString())
                .bind(0, vectorStr)
                .bind(1, model);

        int bindIndex = 2;
        if (fileIds != null && !fileIds.isEmpty()) {
            for (String fileId : fileIds) {
                spec = spec.bind(bindIndex, fileId);
                bindIndex++;
            }
        }
        spec = spec.bind(bindIndex, topK);

        return spec.map((row, _) -> new VectorSearchHit(
                        row.get("chunk_id", String.class),
                        row.get("file_id", String.class),
                        null, // content 不在此查询中返回
                        row.get("distance", Double.class),
                        row.get("tokens", Integer.class)
                ))
                .all()
                .collectList()
                .block();
    }

    @Override
    public void deleteByFileId(String fileId) {
        String sql = "DELETE FROM ai_vector_chunk WHERE file_id = $1";

        vectorTemplate.getDatabaseClient()
                .sql(sql)
                .bind(0, fileId)
                .fetch()
                .rowsUpdated()
                .block();

        log.debug("Deleted vectors for file: {}", fileId);
    }

    @Override
    public void deleteByChunkIds(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }

        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < chunkIds.size(); i++) {
            joiner.add("$" + (i + 1));
        }

        String sql = "DELETE FROM ai_vector_chunk WHERE chunk_id IN (" + joiner + ")";

        var spec = vectorTemplate.getDatabaseClient()
                .sql(sql);

        for (int i = 0; i < chunkIds.size(); i++) {
            spec = spec.bind(i, chunkIds.get(i));
        }

        spec.fetch().rowsUpdated().block();
        log.debug("Deleted {} vectors", chunkIds.size());
    }

    @Override
    public boolean exists(String chunkId, String model) {
        String sql = "SELECT 1 FROM ai_vector_chunk WHERE chunk_id = $1 AND embedding_model = $2 LIMIT 1";

        return vectorTemplate.getDatabaseClient()
                .sql(sql)
                .bind(0, chunkId)
                .bind(1, model)
                .fetch()
                .first()
                .block() != null;
    }
}
