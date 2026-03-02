package com.yunlbd.flexboot4.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * File Embedding Stream 初始化监听器
 * 确保 Redis Stream 在应用启动时存在
 */
@Component
public class FileEmbeddingStreamInitializer {

    private static final Logger log = LoggerFactory.getLogger(FileEmbeddingStreamInitializer.class);

    private final StringRedisTemplate redisTemplate;
    private final FileEmbeddingStreamProperties properties;

    public FileEmbeddingStreamInitializer(StringRedisTemplate redisTemplate,
                                          FileEmbeddingStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initStream() {
        String key = properties.key();
        if (key == null || key.isBlank()) {
            log.warn("File embedding stream key not configured, skipping initialization");
            return;
        }

        try {
            ensureStreamAndGroup(key);
        } catch (Exception e) {
            log.error("Failed to initialize file embedding stream: {}", key, e);
        }
    }

    private void ensureStreamAndGroup(String key) {
        RedisConnectionFactory cf = Objects.requireNonNull(redisTemplate.getConnectionFactory());
        try (RedisConnection conn = cf.getConnection()) {
            byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
            if (keyBytes == null) {
                throw new IllegalStateException("Redis key serialization returned null for key: " + key);
            }

            DataType type = conn.keyCommands().type(keyBytes);
            if (type != DataType.NONE && type != DataType.STREAM) {
                log.error("Redis key '{}' is type {}, expected stream. Skip stream initialization.", key, type);
                return;
            }

            // 创建 Consumer Group
            String group = properties.group();
            if (group != null && !group.isBlank()) {
                try {
                    conn.streamCommands().xGroupCreate(keyBytes, group,
                            org.springframework.data.redis.connection.stream.ReadOffset.from("0"), true);
                    log.info("Created consumer group '{}' for stream: {}", group, key);
                } catch (Exception e) {
                    // Group 可能已存在
                    String msg = e.getMessage();
                    if (msg != null && (msg.contains("BUSYGROUP") || msg.contains("already exists"))) {
                        log.info("Consumer group '{}' already exists for stream: {}", group, key);
                    } else {
                        log.warn("Failed to create consumer group for stream {}: {} ({})", key, msg, e.getClass().getSimpleName());
                        log.debug("Consumer group creation error detail for stream {}", key, e);
                    }
                }
            }

            log.info("File embedding stream initialization completed: {}", key);
        } catch (Exception e) {
            log.error("Failed to initialize file embedding stream: {}", key, e);
        }
    }
}
