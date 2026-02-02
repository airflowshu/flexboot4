package com.yunlbd.flexboot4.aigateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.aigateway.config.RagProperties;
import com.yunlbd.flexboot4.aigateway.dto.RagChatRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RagChatServiceTest {

    @Test
    void chatStreamData_extractsDataLines() {
        EmbeddingHttpClient embeddingHttpClient = mock(EmbeddingHttpClient.class);
        RagRetrievalService ragRetrievalService = mock(RagRetrievalService.class);
        LlmProxyClient llmProxyClient = mock(LlmProxyClient.class);

        when(embeddingHttpClient.embedOne(any(), any())).thenReturn(Mono.just(List.of(0.1f, 0.2f)));
        when(ragRetrievalService.retrieve(any(), any(), any(), anyInt())).thenReturn(Flux.empty());
        when(llmProxyClient.chatStreamRaw(any(), anyMap())).thenReturn(Flux.just(
                "data: {\"id\":\"1\"}\n\n",
                "data: [DONE]\n\n"
        ));

        RagChatService service = new RagChatService(
                embeddingHttpClient,
                ragRetrievalService,
                llmProxyClient,
                new ObjectMapper(),
                new RagProperties("bge-m3", 5, 8000, "sys")
        );

        RagChatRequest req = new RagChatRequest();
        req.setQuery("hello");
        req.setModel("m");

        StepVerifier.create(service.chatStreamData(req, Map.of()).timeout(Duration.ofSeconds(2)))
                .expectNext("{\"id\":\"1\"}")
                .expectNext("[DONE]")
                .verifyComplete();
    }
}
