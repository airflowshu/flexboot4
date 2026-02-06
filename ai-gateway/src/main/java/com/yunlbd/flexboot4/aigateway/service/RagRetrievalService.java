package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.dto.FileChunkDto;
import com.yunlbd.flexboot4.aigateway.dto.RagRetrievedChunkDto;
import com.yunlbd.flexboot4.aigateway.dto.VectorSearchHitDto;
import com.yunlbd.flexboot4.aigateway.repository.impl.FileChunkRepositoryImpl;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RagRetrievalService {

    private final VectorSearchService vectorSearchService;
    private final FileChunkRepositoryImpl fileChunkRepository;

    // 距离阈值，超过这个值的 chunk 会被过滤掉（距离越小表示越相似）
    // 具体值需要根据向量库类型和实际数据调整
    private static final double DISTANCE_THRESHOLD = 0.8;

    public RagRetrievalService(VectorSearchService vectorSearchService, FileChunkRepositoryImpl fileChunkRepository) {
        this.vectorSearchService = vectorSearchService;
        this.fileChunkRepository = fileChunkRepository;
    }

    public Flux<RagRetrievedChunkDto> retrieve(List<Float> queryVector, String kbId, String embeddingModel, List<String> fileIds, int topK) {
        return vectorSearchService.searchTopK(queryVector, kbId, embeddingModel, fileIds, topK)
                .collectList()
                .flatMapMany(hits -> {
                    List<VectorSearchHitDto> filteredHits = hits.stream()
                            .filter(hit -> hit.getDistance() != null && hit.getDistance() < DISTANCE_THRESHOLD)
                            .toList();

                    List<VectorSearchHitDto> finalHits = !filteredHits.isEmpty()
                            ? filteredHits
                            : hits.stream()
                            .filter(hit -> hit.getDistance() != null)
                            .toList();

                    if (finalHits.isEmpty()) {
                        return Flux.empty();
                    }

                    List<String> chunkIds = finalHits.stream()
                            .map(VectorSearchHitDto::getChunkId)
                            .filter(Objects::nonNull)
                            .toList();

                    return fileChunkRepository.findByIds(chunkIds)
                            .collectMap(FileChunkDto::getId)
                            .flatMapMany(chunkMap -> Flux.fromIterable(finalHits)
                                    .map(hit -> merge(hit, chunkMap))
                                    .filter(Objects::nonNull));
                });
    }

    private RagRetrievedChunkDto merge(VectorSearchHitDto hit, Map<String, FileChunkDto> chunkMap) {
        FileChunkDto chunk = chunkMap.get(hit.getChunkId());
        if (chunk == null) {
            return null;
        }
        return new RagRetrievedChunkDto(
                chunk.getId(),
                chunk.getFileId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getTokenCount(),
                hit.getDistance()
        );
    }
}
