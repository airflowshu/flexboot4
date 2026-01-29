package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.config.FileEmbeddingStreamProperties;
import com.yunlbd.flexboot4.entity.SysFileChunk;
import com.yunlbd.flexboot4.service.FileEmbeddingPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FileEmbeddingPublisherImpl implements FileEmbeddingPublisher {

    private static final Logger log = LoggerFactory.getLogger(FileEmbeddingPublisherImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final FileEmbeddingStreamProperties properties;

    public FileEmbeddingPublisherImpl(StringRedisTemplate redisTemplate,
                                      FileEmbeddingStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void publishChunk(SysFileChunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            return;
        }
        try {
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .ofMap(Map.of(
                            "chunkId", chunk.getId(),
                            "fileId", chunk.getFileId(),
                            "model", chunk.getEmbeddingModel() != null ? chunk.getEmbeddingModel() : "bge-m3",
                            "retryCount", "0"
                    ))
                    .withStreamKey(properties.key());

            redisTemplate.opsForStream().add(record);
            log.debug("Published embedding task for chunk: {}", chunk.getId());
        } catch (Exception e) {
            log.error("Failed to publish embedding task for chunk: {}", chunk.getId(), e);
        }
    }

    @Override
    public void publishChunks(List<SysFileChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (SysFileChunk chunk : chunks) {
            publishChunk(chunk);
        }
    }
}
