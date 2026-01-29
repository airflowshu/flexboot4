package com.yunlbd.flexboot4.service.impl;

import com.yunlbd.flexboot4.config.FileEmbeddingStreamProperties;
import com.yunlbd.flexboot4.service.FileEmbeddingPublisher;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class RedisStreamFileEmbeddingPublisher implements FileEmbeddingPublisher {

    private final StringRedisTemplate redisTemplate;
    private final FileEmbeddingStreamProperties properties;

    public RedisStreamFileEmbeddingPublisher(StringRedisTemplate redisTemplate, FileEmbeddingStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(String fileId, int chunkCount, String embeddingModel) {
        if (fileId == null || fileId.isBlank()) {
            return;
        }
        String key = properties.key();
        if (key == null || key.isBlank()) {
            return;
        }
        Map<String, String> value = Map.of(
                "fileId", fileId,
                "chunkCount", String.valueOf(chunkCount),
                "embeddingModel", Objects.toString(embeddingModel, ""),
                "ts", String.valueOf(System.currentTimeMillis())
        );
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(value)
                .withStreamKey(key);
        redisTemplate.opsForStream().add(record);
    }
}

