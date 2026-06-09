package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.config.LlmProxyProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmProxyClientTest {

    @Test
    void applyHeaders_usesConfiguredBearerTokenAndDoesNotForwardInternalApiKey() {
        LlmProxyClient client = new LlmProxyClient(
                new LlmProxyProperties(
                        "http://localhost:11434",
                        "/v1/chat/completions",
                        Duration.ofSeconds(1),
                        "official-key",
                        "deepseek-chat"
                ),
                WebClient.builder()
        );
        HttpHeaders headers = new HttpHeaders();

        client.applyHeaders(headers, Map.of(
                HttpHeaders.AUTHORIZATION, "Bearer user-key",
                "X-AI-API-KEY", "internal-key",
                "X-Trace-Id", "trace-1"
        ));

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer official-key");
        assertThat(headers.getFirst("X-AI-API-KEY")).isNull();
        assertThat(headers.getFirst("X-Trace-Id")).isEqualTo("trace-1");
    }
}
