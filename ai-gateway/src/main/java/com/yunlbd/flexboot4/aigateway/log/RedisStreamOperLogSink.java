package com.yunlbd.flexboot4.aigateway.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.operlog.OperLogRecord;
import com.yunlbd.flexboot4.operlog.OperLogSink;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class RedisStreamOperLogSink implements OperLogSink {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OperLogStreamProperties properties;

    public RedisStreamOperLogSink(ReactiveStringRedisTemplate redisTemplate,
                                  ObjectMapper objectMapper,
                                  OperLogStreamProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public CompletionStage<Void> write(OperLogRecord log) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(log);
        } catch (JsonProcessingException e) {
            return CompletableFuture.completedFuture(null);
        }

        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(Map.of(
                        "eventId", log.eventId() == null ? "" : log.eventId(),
                        "payload", payload
                ))
                .withStreamKey(properties.key());

        return redisTemplate.opsForStream()
                .add(record)
                .then()
                .toFuture();
    }
}
