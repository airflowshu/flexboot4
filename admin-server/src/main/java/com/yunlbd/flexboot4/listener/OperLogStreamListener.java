package com.yunlbd.flexboot4.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.config.OperLogStreamProperties;
import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.operlog.OperLogRecord;
import com.yunlbd.flexboot4.service.SysOperLogService;
import com.yunlbd.flexboot4.util.IpUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class OperLogStreamListener {

    private final StringRedisTemplate redisTemplate;
    private final SysOperLogService sysOperLogService;
    private final ObjectMapper objectMapper;
    private final OperLogStreamProperties properties;
    private final RedisConnectionFactory connectionFactory;

    public OperLogStreamListener(StringRedisTemplate redisTemplate,
                                 SysOperLogService sysOperLogService,
                                 ObjectMapper objectMapper,
                                 OperLogStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.sysOperLogService = sysOperLogService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory());
    }

    @PostConstruct
    public void ensureGroup() {
        String key = properties.key();
        String group = properties.group();
        try (RedisConnection connection = connectionFactory.getConnection()) {
            byte[] keyBytes = redisTemplate.getStringSerializer().serialize(key);
            if (group == null || group.isBlank()) {
                return;
            }
            try {
                connection.streamCommands().xGroupCreate(keyBytes, group, ReadOffset.latest(), true);
            } catch (Exception ignore) {
            }
        } catch (Exception ignore) {
        }
    }

    @SuppressWarnings("unchecked")
    @Scheduled(initialDelay = 15000, fixedDelay = 1000)
    public void poll() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        List<MapRecord<String, String, String>> records;
        try {
            records = ops.read(
                    Consumer.from(properties.group(), properties.consumer()),
                    StreamReadOptions.empty().count(50).block(Duration.ofSeconds(2)),
                    StreamOffset.create(properties.key(), ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            return;
        }
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            try {
                handleRecord(record);
            } catch (Exception e) {
                log.warn("OperLog stream consume failed: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("all")
    private void handleRecord(MapRecord<String, String, String> record) throws Exception {
        Map<String, String> value = record.getValue();
        String payload = value.get("payload");
        if (payload == null || payload.isBlank()) {
            acknowledge(record);
            return;
        }
        OperLogRecord r = objectMapper.readValue(payload, OperLogRecord.class);
        String eventId = r.eventId();
        if (eventId == null || eventId.isBlank()) {
            eventId = value.get("eventId");
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = record.getId().getValue();
        }

        String dedupKey = "operlog:dedup:" + eventId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofDays(properties.dedupTtlDays()));
        if (Boolean.FALSE.equals(first)) {
            acknowledge(record);
            return;
        }

        SysOperLog sysOperLog = new SysOperLog();
        sysOperLog.setTitle(r.title());
        sysOperLog.setBusinessType(r.businessType());
        sysOperLog.setOperatorType(r.operatorType());
        sysOperLog.setMethod(r.method());
        sysOperLog.setRequestMethod(r.requestMethod());
        sysOperLog.setOperUrl(r.operUrl());
        sysOperLog.setOperIp(r.operIp());
        sysOperLog.setTerminal(r.terminal());
        sysOperLog.setOperName(r.operName());
        sysOperLog.setOperUserId(r.operUserId());
        sysOperLog.setDeptId(r.deptId());
        sysOperLog.setStatus(r.status());
        sysOperLog.setErrorMsg(r.errorMsg());
        sysOperLog.setCostTime(r.costTimeMillis());
        sysOperLog.setOperParam(r.operParam());
        sysOperLog.setJsonResult(r.jsonResult());
        sysOperLog.setExtParams(r.extParams());

        if (r.operTimeEpochMillis() > 0) {
            LocalDateTime operTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(r.operTimeEpochMillis()), ZoneId.systemDefault());
            sysOperLog.setOperTime(operTime);
        } else {
            sysOperLog.setOperTime(LocalDateTime.now());
        }

        if (sysOperLog.getOperIp() != null && !sysOperLog.getOperIp().isBlank()) {
            sysOperLog.setOperLocation(IpUtils.getRegion(sysOperLog.getOperIp()));
        }

        sysOperLogService.save(sysOperLog);
        acknowledge(record);
    }

    @SuppressWarnings("all")
    private void acknowledge(MapRecord<String, String, String> record) {
        try {
            redisTemplate.opsForStream().acknowledge(properties.key(), properties.group(), record.getId());
        } catch (Exception ignore) {
        }
    }
}
