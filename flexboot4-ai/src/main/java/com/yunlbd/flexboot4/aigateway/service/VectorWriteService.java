package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.repository.VectorR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static com.yunlbd.flexboot4.aigateway.util.PgVectorUtils.toPgVectorString;

/**
 * 向量写入服务
 */
@Service
public class VectorWriteService {

    private static final Logger log = LoggerFactory.getLogger(VectorWriteService.class);

    private final VectorR2dbcRepository vectorRepository;
    private final R2dbcEntityTemplate r2dbcTemplate;

    public VectorWriteService(VectorR2dbcRepository vectorRepository,
                              @Qualifier("vectorR2dbcEntityTemplate") R2dbcEntityTemplate r2dbcTemplate) {
        this.vectorRepository = vectorRepository;
        this.r2dbcTemplate = r2dbcTemplate;
    }

    /**
     * 保存向量数据
     */
    public Mono<Boolean> saveVector(String kbId, String chunkId, String fileId, String model,
                                     List<Float> vector, Integer tokens) {
        // pgvector 需要字符串格式 '[0.1, 0.2, 0.3]'
        String vectorStr = toPgVectorString(vector);

        // 使用 ON CONFLICT 实现幂等插入
        String sql = """
            INSERT INTO ai_vector_chunk (kb_id, chunk_id, file_id, embedding_model, vector, tokens, created_at)
            VALUES ($1, $2, $3, $4, $5::vector, $6, $7)
            ON CONFLICT (kb_id, chunk_id, embedding_model) DO UPDATE SET
                vector = EXCLUDED.vector,
                tokens = EXCLUDED.tokens,
                created_at = EXCLUDED.created_at
            """;

        return r2dbcTemplate.getDatabaseClient()
                .sql(sql)
                .bind(0, kbId == null ? "" : kbId)
                .bind(1, chunkId)
                .bind(2, fileId)
                .bind(3, model)
                .bind(4, vectorStr)
                .bind(5, tokens)
                .bind(6, LocalDateTime.now())
                .fetch()
                .rowsUpdated()
                .map(rows -> rows > 0)
                .doOnSuccess(r -> log.debug("Saved vector for chunk: {}", chunkId))
                .doOnError(e -> log.error("Failed to save vector for chunk: {}", chunkId, e));
    }
}
