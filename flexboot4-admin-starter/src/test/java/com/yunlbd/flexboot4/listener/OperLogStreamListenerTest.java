package com.yunlbd.flexboot4.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.config.OperLogStreamProperties;
import com.yunlbd.flexboot4.entity.ops.SysOperLog;
import com.yunlbd.flexboot4.lock.NoopDistributedLockService;
import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import com.yunlbd.flexboot4.operlog.OperLogRecord;
import com.yunlbd.flexboot4.service.ops.SysOperLogService;
import com.yunlbd.flexboot4.util.UserAgentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperLogStreamListenerTest {

    private static final String STREAM_KEY = "operlog:stream";
    private static final String DEAD_LETTER_KEY = "operlog:stream:dead";
    private static final String GROUP = "operlog-group";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SysOperLogService sysOperLogService = mock(SysOperLogService.class);
    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final StreamOperations<String, String, String> streamOperations = mock(StreamOperations.class);
    private final UserAgentService userAgentService = mock(UserAgentService.class);
    private final MetricsRecorder metricsRecorder = mock(MetricsRecorder.class);
    private final OperLogStreamListener listener;

    OperLogStreamListenerTest() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(userAgentService.parse(any())).thenReturn(Map.of("browser", "JUnit"));

        listener = new OperLogStreamListener(
                redisTemplate,
                sysOperLogService,
                objectMapper,
                new OperLogStreamProperties(STREAM_KEY, GROUP, "consumer-1", DEAD_LETTER_KEY, 50, 60000, 10),
                userAgentService,
                new NoopDistributedLockService(),
                metricsRecorder
        );
    }

    @Test
    void handleRecordShouldPersistEventIdAndAckAfterSave() throws Exception {
        MapRecord<String, String, String> record = record("1700000000000-0", "evt-1");

        listener.handleRecord(record);

        ArgumentCaptor<SysOperLog> logCaptor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).save(logCaptor.capture());
        assertEquals("evt-1", logCaptor.getValue().getEventId());
        assertEquals("登录", logCaptor.getValue().getTitle());
        assertNotNull(logCaptor.getValue().getOperTime());
        verify(streamOperations).acknowledge(STREAM_KEY, GROUP, RecordId.of("1700000000000-0"));
        verify(metricsRecorder).increment("flexboot4.operlog.stream.persisted", Map.of("status", "success"));
    }

    @Test
    void handleRecordShouldNotAckWhenPersistenceFails() throws Exception {
        MapRecord<String, String, String> record = record("1700000000000-1", "evt-fail");
        doThrow(new IllegalStateException("database down")).when(sysOperLogService).save(any(SysOperLog.class));

        listener.handleRecord(record);

        verify(streamOperations, never()).acknowledge(any(String.class), any(String.class), any(RecordId.class));
        verify(metricsRecorder).increment(eq("flexboot4.operlog.stream.persist_failed"), anyMetricTags());
    }

    @Test
    void handleRecordShouldAckDuplicateEvent() throws Exception {
        MapRecord<String, String, String> record = record("1700000000000-2", "evt-dup");
        doThrow(new DuplicateKeyException("duplicate key value violates unique constraint \"uk_sys_oper_log_event_id\""))
                .when(sysOperLogService).save(any(SysOperLog.class));

        listener.handleRecord(record);

        verify(streamOperations).acknowledge(STREAM_KEY, GROUP, RecordId.of("1700000000000-2"));
        verify(metricsRecorder).increment("flexboot4.operlog.stream.duplicate", Map.of("eventId", "evt-dup"));
    }

    @Test
    void handleRecordShouldKeepPendingForNonEventIntegrityViolation() throws Exception {
        MapRecord<String, String, String> record = record("1700000000000-3", "evt-integrity");
        doThrow(new DataIntegrityViolationException("not null constraint failed"))
                .when(sysOperLogService).save(any(SysOperLog.class));

        listener.handleRecord(record);

        verify(streamOperations, never()).acknowledge(any(String.class), any(String.class), any(RecordId.class));
    }

    @Test
    void handleRecordShouldDeadLetterInvalidPayloadAndAckSourceRecord() {
        MapRecord<String, String, String> record = StreamRecords.mapBacked(Map.of(
                        "eventId", "evt-bad-json",
                        "payload", "{bad-json"
                ))
                .withStreamKey(STREAM_KEY)
                .withId(RecordId.of("1700000000000-31"));

        listener.handleRecord(record);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> deadLetterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(DEAD_LETTER_KEY), deadLetterCaptor.capture());
        assertEquals("payload_invalid", deadLetterCaptor.getValue().get("deadLetterReason"));
        assertEquals("1700000000000-31", deadLetterCaptor.getValue().get("sourceRecordId"));
        verify(streamOperations).acknowledge(STREAM_KEY, GROUP, RecordId.of("1700000000000-31"));
        verify(sysOperLogService, never()).save(any(SysOperLog.class));
        verify(metricsRecorder).increment("flexboot4.operlog.stream.dead_lettered", Map.of("reason", "payload_invalid"));
    }

    @Test
    void reclaimPendingShouldClaimAndProcessIdleMessages() throws Exception {
        RecordId pendingId = RecordId.of("1700000000000-4");
        PendingMessages pendingMessages = new PendingMessages(
                GROUP,
                List.of(new PendingMessage(pendingId, Consumer.from(GROUP, "old-consumer"), Duration.ofSeconds(90), 2))
        );
        MapRecord<String, String, String> claimedRecord = record(pendingId.getValue(), "evt-reclaimed");
        when(streamOperations.pending(any(String.class), any(String.class), any(), any(Long.class), any(Duration.class)))
                .thenReturn(pendingMessages);
        when(streamOperations.claim(any(String.class), any(String.class), any(String.class), any()))
                .thenReturn(List.of(claimedRecord));

        listener.reclaimPending();

        ArgumentCaptor<SysOperLog> logCaptor = ArgumentCaptor.forClass(SysOperLog.class);
        verify(sysOperLogService).save(logCaptor.capture());
        assertEquals("evt-reclaimed", logCaptor.getValue().getEventId());
        verify(streamOperations).acknowledge(STREAM_KEY, GROUP, pendingId);
        verify(metricsRecorder).increment("flexboot4.operlog.stream.reclaimed", Map.of("count", "1"));
    }

    @Test
    void reclaimPendingShouldMoveExceededDeliveryMessagesToDeadLetter() throws Exception {
        RecordId pendingId = RecordId.of("1700000000000-5");
        PendingMessages pendingMessages = new PendingMessages(
                GROUP,
                List.of(new PendingMessage(pendingId, Consumer.from(GROUP, "old-consumer"), Duration.ofSeconds(90), 11))
        );
        MapRecord<String, String, String> claimedRecord = record(pendingId.getValue(), "evt-dead");
        when(streamOperations.pending(any(String.class), any(String.class), any(), any(Long.class), any(Duration.class)))
                .thenReturn(pendingMessages);
        when(streamOperations.claim(any(String.class), any(String.class), any(String.class), any()))
                .thenReturn(List.of(claimedRecord));

        listener.reclaimPending();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> deadLetterCaptor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq(DEAD_LETTER_KEY), deadLetterCaptor.capture());
        Map<String, String> deadLetter = deadLetterCaptor.getValue();
        assertEquals("max_delivery_attempts", deadLetter.get("deadLetterReason"));
        assertEquals("1700000000000-5", deadLetter.get("sourceRecordId"));
        assertEquals("11", deadLetter.get("deliveries"));
        assertEquals("evt-dead", deadLetter.get("eventId"));
        verify(streamOperations).acknowledge(STREAM_KEY, GROUP, pendingId);
        verify(sysOperLogService, never()).save(any(SysOperLog.class));
        verify(metricsRecorder).increment("flexboot4.operlog.stream.dead_lettered", Map.of("reason", "max_delivery_attempts"));
    }

    private MapRecord<String, String, String> record(String redisId, String eventId) throws Exception {
        OperLogRecord payload = new OperLogRecord(
                eventId,
                "登录",
                8,
                1,
                "AuthController.login",
                "POST",
                "/api/admin/auth/login",
                "127.0.0.1",
                Map.of("userAgent", "JUnit"),
                "admin",
                "1",
                "dept-1",
                1700000000000L,
                12L,
                0,
                null,
                Map.of("username", "admin"),
                Map.of("code", 200),
                Map.of("source", "test")
        );
        return StreamRecords.mapBacked(Map.of(
                        "eventId", eventId,
                        "payload", objectMapper.writeValueAsString(payload)
                ))
                .withStreamKey(STREAM_KEY)
                .withId(RecordId.of(redisId));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> anyMetricTags() {
        return any(Map.class);
    }
}
