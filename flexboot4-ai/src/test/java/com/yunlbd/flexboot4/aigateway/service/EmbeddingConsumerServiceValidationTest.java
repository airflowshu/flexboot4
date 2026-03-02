package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.config.EmbeddingStreamProperties;
import com.yunlbd.flexboot4.aigateway.repository.impl.FileChunkRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmbeddingConsumerServiceValidationTest {

    @Test
    void onMessage_whenMissingRequiredFields_ackAndDeleteAndSkipProcessing() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveStreamOperations<String, Object, Object> streamOps =
                (ReactiveStreamOperations<String, Object, Object>) mock(ReactiveStreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        when(streamOps.acknowledge(anyString(), anyString(), anyString())).thenReturn(Mono.just(1L));
        when(streamOps.delete(anyString(), anyString())).thenReturn(Mono.just(1L));

        EmbeddingStreamProperties props = new EmbeddingStreamProperties(
                "file-embedding:stream",
                "file-embedding:stream:dlq",
                "file-embedding-group",
                "ai-gateway-1"
        );

        EmbeddingHttpClient embeddingHttpClient = mock(EmbeddingHttpClient.class);
        VectorWriteService vectorWriteService = mock(VectorWriteService.class);
        FileChunkRepositoryImpl chunkRepository = mock(FileChunkRepositoryImpl.class);

        EmbeddingConsumerService service = new EmbeddingConsumerService(
                redisTemplate,
                props,
                embeddingHttpClient,
                vectorWriteService,
                chunkRepository
        );

        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(Map.of("init", "stream-init"))
                .withStreamKey(props.key())
                .withId(RecordId.of("1-0"));

        service.onMessage(record);

        verify(streamOps, times(1)).acknowledge(eq(props.key()), eq(props.group()), eq("1-0"));
        verify(streamOps, times(1)).delete(eq(props.key()), eq("1-0"));
        verifyNoInteractions(chunkRepository, embeddingHttpClient, vectorWriteService);
    }
}
