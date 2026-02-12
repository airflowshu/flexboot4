package com.yunlbd.flexboot4.service.kb;

import com.yunlbd.flexboot4.config.FileEmbeddingStreamProperties;
import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Primary
public class RedisStreamFileEmbeddingPublisher implements FileEmbeddingPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamFileEmbeddingPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final FileEmbeddingStreamProperties properties;

    public RedisStreamFileEmbeddingPublisher(StringRedisTemplate redisTemplate, FileEmbeddingStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void publishChunk(SysFileChunk chunk) {
        if (chunk == null || chunk.getId() == null) {
            return;
        }
        try {
            String key = properties.key();
            if (key == null || key.isBlank()) {
                return;
            }
            String model = chunk.getEmbeddingModel() != null ? chunk.getEmbeddingModel() : "bge-m3";
            String fileId = chunk.getFileId() != null ? chunk.getFileId() : "";
            MapRecord<String, String, String> record = StreamRecords.newRecord()
                    .ofMap(Map.of(
                            "chunkId", chunk.getId(),
                            "fileId", fileId,
                            "model", model,
                            "retryCount", "0"
                    ))
                    .withStreamKey(key);
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

