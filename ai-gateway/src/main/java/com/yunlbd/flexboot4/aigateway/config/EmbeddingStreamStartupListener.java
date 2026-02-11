package com.yunlbd.flexboot4.aigateway.config;

import com.yunlbd.flexboot4.aigateway.service.EmbeddingConsumerService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Embedding Stream 启动监听器
 * 使用简单轮询方式消费 Redis Stream
 */
@Component
public class EmbeddingStreamStartupListener {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingStreamStartupListener.class);

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingConsumerService consumerService;
    private final EmbeddingStreamProperties streamProperties;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public EmbeddingStreamStartupListener(StringRedisTemplate redisTemplate,
                                           EmbeddingConsumerService consumerService,
                                           EmbeddingStreamProperties streamProperties) {
        this.redisTemplate = redisTemplate;
        this.consumerService = consumerService;
        this.streamProperties = streamProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {
        // 先确保 Consumer Group 存在
        ensureConsumerGroup();

        if (running.compareAndSet(false, true)) {
            log.info("Starting embedding consumer: stream={}, group={}, consumer={}",
                    streamProperties.key(), streamProperties.group(), streamProperties.consumer());
            log.info("Embedding consumer started successfully (polling mode)");
        }
    }

    /**
     * 定时轮询消费消息
     */
    @Scheduled(fixedDelayString = "${embedding.stream.poll-interval:1000}")
    public void pollMessages() {
        if (!running.get()) {
            return;
        }

        String key = streamProperties.key();
        // 检查 Stream 是否存在，不存在则跳过
        Boolean exists = redisTemplate.hasKey(key);
        if (exists == null || !exists) {
            return;
        }

        try {
            Consumer consumer = Consumer.from(streamProperties.group(), streamProperties.consumer());
            StreamOffset<String> offset = StreamOffset.create(streamProperties.key(), ReadOffset.lastConsumed());

            // 读取消息
            @SuppressWarnings("unchecked")
            List<MapRecord<String, String, String>> messages = (List) redisTemplate.opsForStream()
                    .read(consumer, offset);

            for (MapRecord<String, String, String> record : messages) {
                log.debug("Received message: {}", record.getId());
                try {
                    consumerService.onMessage(record);
                } catch (Exception e) {
                    log.error("Error processing message: {}", record.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during stream poll", e);
        }
    }

    private void ensureConsumerGroup() {
        try {
            String key = streamProperties.key();
            String group = streamProperties.group();

            log.info("Creating consumer group '{}' for stream '{}'", group, key);

            // 尝试创建 Consumer Group
            redisTemplate.opsForStream().createGroup(key, ReadOffset.from("0"), group);
            log.info("Consumer group created successfully: {}", group);
        } catch (Exception e) {
            String msg = e.getMessage();
            // 检查是否是 group 已存在
            if (msg != null && (msg.contains("BUSYGROUP") || msg.contains("already exists") || msg.contains("BUSY"))) {
                log.info("Consumer group already exists: {}", streamProperties.group());
            } else {
                // 检查是否是 stream 不存在导致的问题
                log.warn("Failed to create consumer group: {} (stream may not exist yet, will be auto-created on first message)", msg);
            }
        }
    }

    @PreDestroy
    public void stopConsumer() {
        if (running.compareAndSet(true, false)) {
            log.info("Embedding consumer stopped");
        }
    }
}
