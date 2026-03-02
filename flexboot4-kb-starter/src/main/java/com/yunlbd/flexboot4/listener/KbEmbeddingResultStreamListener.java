package com.yunlbd.flexboot4.listener;

import com.yunlbd.flexboot4.config.FileEmbeddingStreamProperties;
import com.yunlbd.flexboot4.entity.kb.SysFileChunk;
import com.yunlbd.flexboot4.entity.sys.SysFile;
import com.yunlbd.flexboot4.service.kb.SysFileChunkService;
import com.yunlbd.flexboot4.service.sys.SysFileService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class KbEmbeddingResultStreamListener {

    private static final String GROUP = "kb-embedding-result-group";
    private static final String CONSUMER = "admin-server-1";
    private static final Duration PROGRESS_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final FileEmbeddingStreamProperties properties;
    private final SysFileService sysFileService;
    private final SysFileChunkService sysFileChunkService;

    public KbEmbeddingResultStreamListener(StringRedisTemplate redisTemplate,
                                          FileEmbeddingStreamProperties properties,
                                          SysFileService sysFileService,
                                          SysFileChunkService sysFileChunkService) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.sysFileService = sysFileService;
        this.sysFileChunkService = sysFileChunkService;
        this.connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
    }

    private String resultStreamKey() {
        String key = properties.key();
        return key == null ? null : key + ":result";
    }

    @PostConstruct
    public void ensureGroup() {
        String key = resultStreamKey();
        if (key == null || key.isBlank()) {
            return;
        }
        try (RedisConnection connection = connectionFactory.getConnection()) {
            byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
            try {
                connection.streamCommands().xGroupCreate(keyBytes, GROUP, ReadOffset.latest(), true);
            } catch (Exception ignore) {
            }
        } catch (Exception ignore) {
        }
    }

    @SuppressWarnings("unchecked")
    @Scheduled(initialDelay = 15000, fixedDelay = 1000)
    public void poll() {
        String key = resultStreamKey();
        if (key == null || key.isBlank()) {
            return;
        }

        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        List<MapRecord<String, String, String>> records;
        try {
            records = ops.read(
                    Consumer.from(GROUP, CONSUMER),
                    StreamReadOptions.empty().count(200).block(Duration.ofSeconds(2)),
                    StreamOffset.create(key, ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            return;
        }
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            try {
                handleRecord(key, record);
            } catch (Exception e) {
                log.warn("KB embedding result consume failed: {}", e.getMessage());
            }
        }
    }

    private void handleRecord(String streamKey, MapRecord<String, String, String> record) {
        Map<String, String> v = record.getValue();
        String kbId = v.get("kbId");
        String fileId = v.get("fileId");
        String chunkId = v.get("chunkId");
        String success = v.get("success");

        if (kbId == null || kbId.isBlank() || fileId == null || fileId.isBlank() || chunkId == null || chunkId.isBlank()) {
            acknowledge(streamKey, record);
            return;
        }

        boolean ok = "1".equals(success) || "true".equalsIgnoreCase(success);
        if (!ok) {
            clearProgress(kbId, fileId);
            acknowledge(streamKey, record);
            return;
        }

        String progressKey = progressKey(kbId, fileId);
        redisTemplate.opsForSet().add(progressKey, chunkId);
        redisTemplate.expire(progressKey, PROGRESS_TTL);

        long expected = expectedChunks(fileId);
        Long current = redisTemplate.opsForSet().size(progressKey);
        long done = current == null ? 0L : current;

        if (expected > 0 && done >= expected) {
            clearProgress(kbId, fileId);
        }

        acknowledge(streamKey, record);
    }

    private long expectedChunks(String fileId) {
        SysFile f = sysFileService.getById(fileId);
        if (f != null && f.getChunkCount() != null && f.getChunkCount() > 0) {
            return f.getChunkCount();
        }
        return sysFileChunkService.count(com.mybatisflex.core.query.QueryWrapper.create()
                .from(SysFileChunk.class)
                .where(SysFileChunk::getFileId).eq(fileId));
    }

    private String progressKey(String kbId, String fileId) {
        return "kb:index:ok:" + kbId + ":" + fileId;
    }

    private void clearProgress(String kbId, String fileId) {
        redisTemplate.delete(progressKey(kbId, fileId));
    }

    private void acknowledge(String streamKey, MapRecord<String, String, String> record) {
        try {
            redisTemplate.opsForStream().acknowledge(streamKey, GROUP, record.getId());
        } catch (Exception ignore) {
        }
    }
}

