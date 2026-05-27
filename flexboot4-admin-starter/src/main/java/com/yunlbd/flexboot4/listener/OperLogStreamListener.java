package com.yunlbd.flexboot4.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.config.OperLogStreamProperties;
import com.yunlbd.flexboot4.entity.ops.SysOperLog;
import com.yunlbd.flexboot4.lock.DistributedLockService;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.operlog.OperLogRecord;
import com.yunlbd.flexboot4.service.ops.SysOperLogService;
import com.yunlbd.flexboot4.util.IpUtils;
import com.yunlbd.flexboot4.util.UserAgentService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class OperLogStreamListener {

    private final StringRedisTemplate redisTemplate;
    private final SysOperLogService sysOperLogService;
    private final ObjectMapper objectMapper;
    private final OperLogStreamProperties properties;
    private final UserAgentService userAgentService;
    private final RedisConnectionFactory connectionFactory;
    private final DistributedLockService distributedLockService;
    private final MetricsRecorder metricsRecorder;

    public OperLogStreamListener(StringRedisTemplate redisTemplate,
                                 SysOperLogService sysOperLogService,
                                 ObjectMapper objectMapper,
                                 OperLogStreamProperties properties,
                                 UserAgentService userAgentService,
                                 DistributedLockService distributedLockService,
                                 MetricsRecorder metricsRecorder) {
        this.redisTemplate = redisTemplate;
        this.sysOperLogService = sysOperLogService;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.userAgentService = userAgentService;
        this.distributedLockService = distributedLockService;
        this.metricsRecorder = metricsRecorder;
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
            metricsRecorder.increment("flexboot4.operlog.stream.poll_failed", Map.of("exception", e.getClass().getSimpleName()));
            return;
        }
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            handleRecord(record);
        }
    }

    @Scheduled(initialDelay = 30000, fixedDelayString = "${operlog.stream.reclaim-min-idle-millis:60000}")
    public void reclaimPending() {
        distributedLockService.executeIfLocked(
                "admin:operlog:reclaim-pending",
                Duration.ofMillis(Math.max(properties.reclaimMinIdleMillis(), 1)),
                this::doReclaimPending
        );
    }

    void doReclaimPending() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        PendingMessages pendingMessages;
        try {
            pendingMessages = ops.pending(
                    properties.key(),
                    properties.group(),
                    Range.unbounded(),
                    Math.max(properties.reclaimBatchSize(), 1),
                    Duration.ofMillis(Math.max(properties.reclaimMinIdleMillis(), 1))
            );
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.pending_scan_failed", Map.of("exception", e.getClass().getSimpleName()));
            log.debug("OperLog pending scan failed: {}", e.getMessage());
            return;
        }
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        List<RecordId> reclaimIds = new ArrayList<>();
        List<PendingMessage> deadLetterCandidates = new ArrayList<>();
        for (PendingMessage pendingMessage : pendingMessages) {
            if (properties.maxDeliveryAttempts() > 0
                    && pendingMessage.getTotalDeliveryCount() > properties.maxDeliveryAttempts()) {
                log.warn("OperLog pending message exceeded max delivery attempts, id={}, deliveries={}",
                        pendingMessage.getIdAsString(), pendingMessage.getTotalDeliveryCount());
                deadLetterCandidates.add(pendingMessage);
                continue;
            }
            reclaimIds.add(pendingMessage.getId());
        }
        movePendingToDeadLetter(ops, deadLetterCandidates);
        if (reclaimIds.isEmpty()) {
            return;
        }

        List<MapRecord<String, String, String>> claimed;
        try {
            claimed = ops.claim(
                    properties.key(),
                    properties.group(),
                    properties.consumer(),
                    RedisStreamCommands.XClaimOptions
                            .minIdle(Duration.ofMillis(Math.max(properties.reclaimMinIdleMillis(), 1)))
                            .ids(reclaimIds)
            );
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.claim_failed", Map.of("exception", e.getClass().getSimpleName()));
            log.debug("OperLog pending claim failed: {}", e.getMessage());
            return;
        }
        metricsRecorder.increment("flexboot4.operlog.stream.reclaimed", Map.of("count", Integer.toString(claimed == null ? 0 : claimed.size())));
        processRecords(claimed);
    }

    void handleRecord(MapRecord<String, String, String> record) {
        Map<String, String> value = record.getValue();
        String payload = value.get("payload");
        if (payload == null || payload.isBlank()) {
            acknowledge(record);
            return;
        }
        OperLogRecord r;
        try {
            r = objectMapper.readValue(payload, OperLogRecord.class);
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.payload_invalid", Map.of("exception", e.getClass().getSimpleName()));
            log.warn("OperLog stream payload parse failed, id={}: {}", record.getId().getValue(), e.getMessage());
            moveToDeadLetter(record, "payload_invalid", e, null);
            return;
        }
        String eventId = r.eventId();
        if (eventId == null || eventId.isBlank()) {
            eventId = value.get("eventId");
        }
        if (eventId == null || eventId.isBlank()) {
            eventId = record.getId().getValue();
        }

        SysOperLog sysOperLog = new SysOperLog();
        sysOperLog.setEventId(eventId);
        sysOperLog.setTitle(r.title());
        sysOperLog.setBusinessType(r.businessType());
        sysOperLog.setOperatorType(r.operatorType());
        sysOperLog.setMethod(r.method());
        sysOperLog.setRequestMethod(r.requestMethod());
        sysOperLog.setOperUrl(r.operUrl());
        sysOperLog.setOperIp(r.operIp());
        
        // 处理终端信息解析
        Map<String, String> terminal = r.terminal();
        if (terminal != null && terminal.containsKey("userAgent")) {
            String ua = terminal.get("userAgent");
            Map<String, String> parsed = userAgentService.parse(ua);
            if (parsed != null && !parsed.isEmpty()) {
                // 合并解析后的信息，保留原始 userAgent 以防万一
                Map<String, String> merged = new HashMap<>(parsed);
                merged.put("userAgent", ua);
                sysOperLog.setTerminal(merged);
            } else {
                sysOperLog.setTerminal(terminal);
            }
        } else {
            sysOperLog.setTerminal(terminal);
        }
        
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

        Instant start = Instant.now();
        try {
            sysOperLogService.save(sysOperLog);
            metricsRecorder.increment("flexboot4.operlog.stream.persisted", Map.of("status", "success"));
            metricsRecorder.recordDuration("flexboot4.operlog.stream.persist_duration", Duration.between(start, Instant.now()), Map.of("status", "success"));
            acknowledge(record);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEvent(e)) {
                metricsRecorder.increment("flexboot4.operlog.stream.duplicate", Map.of("eventId", eventId));
                log.debug("OperLog stream duplicate event ignored, eventId={}", eventId);
                acknowledge(record);
                return;
            }
            metricsRecorder.increment("flexboot4.operlog.stream.persist_failed", Map.of("exception", e.getClass().getSimpleName()));
            metricsRecorder.recordDuration("flexboot4.operlog.stream.persist_duration", Duration.between(start, Instant.now()), Map.of("status", "failed"));
            log.warn("OperLog stream persistence failed, id={}, eventId={}: {}",
                    record.getId().getValue(), eventId, e.getMessage());
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.persist_failed", Map.of("exception", e.getClass().getSimpleName()));
            metricsRecorder.recordDuration("flexboot4.operlog.stream.persist_duration", Duration.between(start, Instant.now()), Map.of("status", "failed"));
            log.warn("OperLog stream persistence failed, id={}, eventId={}: {}",
                    record.getId().getValue(), eventId, e.getMessage());
        }
    }

    private void processRecords(List<MapRecord<String, String, String>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            handleRecord(record);
        }
    }

    private void movePendingToDeadLetter(StreamOperations<String, String, String> ops,
                                         List<PendingMessage> deadLetterCandidates) {
        if (deadLetterCandidates == null || deadLetterCandidates.isEmpty()) {
            return;
        }

        List<RecordId> ids = deadLetterCandidates.stream()
                .map(PendingMessage::getId)
                .toList();
        List<MapRecord<String, String, String>> claimed;
        try {
            claimed = ops.claim(
                    properties.key(),
                    properties.group(),
                    properties.consumer(),
                    RedisStreamCommands.XClaimOptions
                            .minIdle(Duration.ofMillis(Math.max(properties.reclaimMinIdleMillis(), 1)))
                            .ids(ids)
            );
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.dead_letter_claim_failed", Map.of("exception", e.getClass().getSimpleName()));
            log.warn("OperLog pending dead-letter claim failed, ids={}: {}", ids, e.getMessage());
            return;
        }

        if (claimed == null || claimed.isEmpty()) {
            metricsRecorder.increment("flexboot4.operlog.stream.dead_letter_empty_claim", Map.of("count", Integer.toString(ids.size())));
            return;
        }

        Map<String, String> deliveriesById = new HashMap<>();
        for (PendingMessage pendingMessage : deadLetterCandidates) {
            deliveriesById.put(pendingMessage.getIdAsString(), Long.toString(pendingMessage.getTotalDeliveryCount()));
        }
        for (MapRecord<String, String, String> record : claimed) {
            moveToDeadLetter(record, "max_delivery_attempts", null, deliveriesById.get(record.getId().getValue()));
        }
    }

    private boolean moveToDeadLetter(MapRecord<String, String, String> record,
                                     String reason,
                                     Exception exception,
                                     String deliveries) {
        Map<String, String> deadLetter = new HashMap<>(record.getValue());
        deadLetter.put("sourceStream", properties.key());
        deadLetter.put("sourceGroup", properties.group());
        deadLetter.put("sourceConsumer", properties.consumer());
        deadLetter.put("sourceRecordId", record.getId().getValue());
        deadLetter.put("deadLetterReason", reason);
        deadLetter.put("deadLetterAt", Instant.now().toString());
        if (deliveries != null && !deliveries.isBlank()) {
            deadLetter.put("deliveries", deliveries);
        }
        if (exception != null) {
            deadLetter.put("exception", exception.getClass().getSimpleName());
            if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
                deadLetter.put("exceptionMessage", exception.getMessage());
            }
        }

        try {
            redisTemplate.opsForStream().add(properties.deadLetterKey(), deadLetter);
            metricsRecorder.increment("flexboot4.operlog.stream.dead_lettered", Map.of("reason", reason));
            acknowledge(record);
            return true;
        } catch (Exception e) {
            metricsRecorder.increment("flexboot4.operlog.stream.dead_letter_failed", Map.of("exception", e.getClass().getSimpleName(), "reason", reason));
            log.warn("OperLog stream dead-letter write failed, id={}, reason={}: {}",
                    record.getId().getValue(), reason, e.getMessage());
            return false;
        }
    }

    private boolean isDuplicateEvent(Exception e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("uk_sys_oper_log_event_id")) {
                return true;
            }
            current = current.getCause();
        }
        return e instanceof DuplicateKeyException;
    }

    @SuppressWarnings("all")
    private void acknowledge(MapRecord<String, String, String> record) {
        try {
            redisTemplate.opsForStream().acknowledge(properties.key(), properties.group(), record.getId());
        } catch (Exception ignore) {
        }
    }
}
