package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.dto.VectorSearchHitDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.StringJoiner;

import static com.yunlbd.flexboot4.aigateway.util.PgVectorUtils.toPgVectorString;

@Service
public class VectorSearchService {

    private final R2dbcEntityTemplate vectorTemplate;

    public VectorSearchService(@Qualifier("vectorR2dbcEntityTemplate") R2dbcEntityTemplate vectorTemplate) {
        this.vectorTemplate = vectorTemplate;
    }

    public Flux<VectorSearchHitDto> searchTopK(List<Float> queryVector, String kbId, String embeddingModel, List<String> fileIds, int topK) {
        if (topK <= 0) {
            return Flux.empty();
        }

        String vectorStr = toPgVectorString(queryVector);
        StringBuilder sql = new StringBuilder("""
            SELECT chunk_id, file_id, tokens, (vector <-> $1::vector) AS distance
            FROM ai_vector_chunk
            WHERE kb_id = $2
              AND embedding_model = $3
            """);

        int nextPlaceholder = 4;
        if (fileIds != null && !fileIds.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (int i = 0; i < fileIds.size(); i++) {
                joiner.add("$" + nextPlaceholder);
                nextPlaceholder++;
            }
            sql.append(" AND file_id IN (").append(joiner).append(")\n");
        }

        sql.append(" ORDER BY vector <-> $1::vector\n");
        sql.append(" LIMIT ?");

        var spec = vectorTemplate.getDatabaseClient()
                .sql(sql.toString())
                .bind(0, vectorStr)
                .bind(1, kbId == null ? "" : kbId)
                .bind(2, embeddingModel);

        int bindIndex = 3;
        if (fileIds != null && !fileIds.isEmpty()) {
            for (String fileId : fileIds) {
                spec = spec.bind(bindIndex, fileId);
                bindIndex++;
            }
        }
        spec = spec.bind(bindIndex, topK);

        return spec.map((row, _) -> new VectorSearchHitDto(
                        row.get("chunk_id", String.class),
                        row.get("file_id", String.class),
                        row.get("tokens", Integer.class),
                        row.get("distance", Double.class)
                ))
                .all();
    }
}
