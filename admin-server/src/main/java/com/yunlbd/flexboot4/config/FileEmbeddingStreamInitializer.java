package com.yunlbd.flexboot4.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
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

            // 发送一个空消息来创建 Stream（使用 XADD，ID为 * 让 Redis 自动生成）
            try {
                byte[][] args = new byte[][]{
                        redisTemplate.getStringSerializer().serialize("*"),
                        redisTemplate.getStringSerializer().serialize("init"),
                        redisTemplate.getStringSerializer().serialize("stream-init")
                };
                conn.execute("XADD", args);
                log.info("Created file embedding stream: {}", key);
            } catch (Exception e) {
                log.debug("Stream may already exist or error creating: {}", key);
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
                        log.warn("Failed to create consumer group for stream {}: {}", key, msg);
                    }
                }
            }

            log.info("File embedding stream initialization completed: {}", key);
        } catch (Exception e) {
            log.error("Failed to initialize file embedding stream: {}", key, e);
        }
    }
}
