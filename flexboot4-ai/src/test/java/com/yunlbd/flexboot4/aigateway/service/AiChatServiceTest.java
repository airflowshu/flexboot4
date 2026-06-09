package com.yunlbd.flexboot4.aigateway.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void chatStreamData_forwardsOpenAiCompatibleBodyWithoutRagRetrieval() {
        LlmProxyClient llmProxyClient = mock(LlmProxyClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(llmProxyClient.defaultModel()).thenReturn("deepseek-chat");
        when(llmProxyClient.chatStreamRaw(any(), anyMap())).thenReturn(Flux.just(
                "data: {\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}\n\n",
                "data: [DONE]\n\n"
        ));

        AiChatService service = new AiChatService(llmProxyClient, objectMapper);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("query", "hello");

        StepVerifier.create(service.chatStreamData(request, Map.of()).timeout(Duration.ofSeconds(2)))
                .expectNext("{\"choices\":[{\"delta\":{\"content\":\"hi\"}}]}")
                .verifyComplete();

        verify(llmProxyClient).chatStreamRaw(any(), anyMap());
    }
}
