package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.config.EmbeddingStreamProperties;
import com.yunlbd.flexboot4.aigateway.repository.impl.FileChunkRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Embedding 消费者服务
 * 职责：消费 Redis Stream，读取 chunk，写入向量（只读 admin 库，只写向量库）
 */
@Service
public class EmbeddingConsumerService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingConsumerService.class);
    private static final int MAX_RETRY = 3;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final EmbeddingStreamProperties streamProperties;
    private final EmbeddingHttpClient embeddingHttpClient;
    private final VectorWriteService vectorWriteService;
    private final FileChunkRepositoryImpl chunkRepository;

    public EmbeddingConsumerService(ReactiveStringRedisTemplate redisTemplate,
                                    EmbeddingStreamProperties streamProperties,
                                    EmbeddingHttpClient embeddingHttpClient,
                                    VectorWriteService vectorWriteService,
                                    FileChunkRepositoryImpl chunkRepository) {
        this.redisTemplate = redisTemplate;
        this.streamProperties = streamProperties;
        this.embeddingHttpClient = embeddingHttpClient;
        this.vectorWriteService = vectorWriteService;
        this.chunkRepository = chunkRepository;
    }

    public void onMessage(MapRecord<String, String, String> message) {
        String chunkId = message.getValue().get("chunkId");
        String fileId = message.getValue().get("fileId");
        String model = message.getValue().get("model");
        String kbId = message.getValue().get("kbId");
        int retryCount = Integer.parseInt(message.getValue().getOrDefault("retryCount", "0"));
        String messageId = message.getId() != null ? message.getId().getValue() : null;

        log.info("Processing embedding task: kbId={}, chunkId={}, fileId={}, model={}", kbId, chunkId, fileId, model);

        processEmbedding(kbId, chunkId, fileId, model, retryCount, messageId)
                .subscribe(
                        success -> {
                            if (success) {
                                log.info("Embedding completed: chunkId={}", chunkId);
                            }
                        },
                        error -> log.error("Embedding failed: chunkId={}", chunkId, error)
                );
    }

    private Mono<Boolean> processEmbedding(String kbId, String chunkId, String fileId, String model,
                                            int retryCount, String messageId) {
        // 1. 查询 chunk 内容（只读）
        return chunkRepository.findById(chunkId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Chunk not found: " + chunkId)))
                .flatMap(chunk -> {
                    // 2. 调用 Embedding 服务
                    return embeddingHttpClient.embedOne(chunk.getContent(), model)
                            .flatMap(vector -> {
                                // 3. 写入向量数据库（幂等写入）
                                return vectorWriteService.saveVector(
                                                kbId,
                                                chunkId,
                                                fileId,
                                                model,
                                                vector,
                                                chunk.getTokenCount()
                                        )
                                        .flatMap(saved -> publishResult(kbId, chunkId, fileId, model, true, null).thenReturn(true));
                            });
                })
                .onErrorResume(error -> {
                    log.error("Embedding failed for chunk {}: {}", chunkId, error.getMessage());

                    if (retryCount < MAX_RETRY) {
                        // 重试：增加重试计数并写回消息
                        return retryMessage(kbId, chunkId, fileId, model, retryCount + 1, messageId);
                    } else {
                        // 超过重试次数：写入 DLQ
                        return writeToDlq(kbId, chunkId, fileId, model, retryCount, error.getMessage())
                                .then(publishResult(kbId, chunkId, fileId, model, false, error.getMessage()))
                                .thenReturn(false);
                    }
                });
    }

    /**
     * 重试消息
     */
    private Mono<Boolean> retryMessage(String kbId, String chunkId, String fileId, String model,
                                        int retryCount, String messageId) {
        MapRecord<String, String, String> newMessage = org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                .ofMap(Map.of(
                        "kbId", kbId == null ? "" : kbId,
                        "chunkId", chunkId,
                        "fileId", fileId,
                        "model", model,
                        "retryCount", String.valueOf(retryCount)
                ))
                .withStreamKey(streamProperties.key());

        return redisTemplate.opsForStream().add(newMessage)
                .then(redisTemplate.opsForStream().delete(streamProperties.key(), messageId))
                .thenReturn(true);
    }

    /**
     * 写入 DLQ
     */
    private Mono<Void> writeToDlq(String kbId, String chunkId, String fileId, String model,
                                   int retryCount, String errorMsg) {
        MapRecord<String, String, String> dlqMessage = org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                .ofMap(Map.of(
                        "kbId", kbId == null ? "" : kbId,
                        "chunkId", chunkId,
                        "fileId", fileId,
                        "model", model,
                        "retryCount", String.valueOf(retryCount),
                        "error", errorMsg
                ))
                .withStreamKey(streamProperties.dlqKey());

        return redisTemplate.opsForStream().add(dlqMessage).then();
    }

    private Mono<Void> publishResult(String kbId, String chunkId, String fileId, String model, boolean success, String errorMsg) {
        String resultKey = streamProperties.key() + ":result";
        MapRecord<String, String, String> result = org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                .ofMap(Map.of(
                        "kbId", kbId == null ? "" : kbId,
                        "chunkId", chunkId == null ? "" : chunkId,
                        "fileId", fileId == null ? "" : fileId,
                        "model", model == null ? "" : model,
                        "success", success ? "1" : "0",
                        "error", errorMsg == null ? "" : errorMsg
                ))
                .withStreamKey(resultKey);
        return redisTemplate.opsForStream().add(result).then();
    }
}
