package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.config.EmbeddingHttpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Embedding HTTP 客户端 (Ollama 兼容)
 */
@Service
public class EmbeddingHttpClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingHttpClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public EmbeddingHttpClient(EmbeddingHttpProperties properties) {
        this.webClient = WebClient.builder()
                .baseUrl(properties.url())
                .build();
        this.timeout = properties.timeout();
    }

    /**
     * 调用 Embedding API (Ollama 格式)
     */
    public Mono<List<List<Float>>> embed(List<String> texts, String model) {
        // Ollama 使用 prompt 字段（单个字符串）
        String prompt = String.join("\n", texts);
        OllamaEmbeddingRequest request = new OllamaEmbeddingRequest(model, prompt);

        return webClient.post()
                .uri("/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaEmbeddingResponse.class)
                .timeout(timeout)
                .map(response -> {
                    if (response.embedding() == null) {
                        throw new RuntimeException("Empty embedding response");
                    }
                    return List.of(response.embedding());
                })
                .doOnError(e -> log.error("Failed to call embedding service", e));
    }

    /**
     * 单文本 embedding
     */
    public Mono<List<Float>> embedOne(String text, String model) {
        return embed(List.of(text), model)
                .map(list -> list.isEmpty() ? List.of() : list.get(0));
    }

    // Ollama 请求格式
    record OllamaEmbeddingRequest(String model, String prompt) {
    }

    // Ollama 响应格式
    record OllamaEmbeddingResponse(String model, List<Float> embedding, Integer prompt_eval_count) {
    }
}
