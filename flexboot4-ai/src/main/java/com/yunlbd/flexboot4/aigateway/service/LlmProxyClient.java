package com.yunlbd.flexboot4.aigateway.service;

import com.yunlbd.flexboot4.aigateway.config.LlmProxyProperties;
import io.netty.channel.ChannelOption;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class LlmProxyClient {

    private static final Logger log = LoggerFactory.getLogger(LlmProxyClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final String chatPath;
    private final String apiKey;
    private final String defaultModel;

    public LlmProxyClient(LlmProxyProperties properties, WebClient.Builder webClientBuilder) {
        this.chatPath = properties.chatPath() == null ? "/v1/chat/completions" : properties.chatPath();
        this.apiKey = normalize(properties.apiKey());
        this.defaultModel = normalize(properties.defaultModel());
        Duration timeout = properties.timeout() == null ? Duration.ofSeconds(120) : properties.timeout();

        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis())
                .responseTimeout(timeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeout.toMillis(), TimeUnit.MILLISECONDS)));

        this.webClient = webClientBuilder
                .baseUrl(properties.url())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Mono<JsonNode> chat(JsonNode body, Map<String, String> headers) {
        String bodyStr = bodyToString(body);
        log.info("=== [LLM] Request to {}. Body: {}", chatPath, bodyStr);

        return webClient.post()
                .uri(chatPath)
                .headers(h -> applyHeaders(h, headers))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(bodyStr)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> 
                    response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("=== [LLM] Chat Error {}: {}", response.statusCode(), errorBody);
                                return Mono.error(new WebClientResponseException(
                                        response.statusCode().value(),
                                        "LLM Chat Error: " + errorBody,
                                        response.headers().asHttpHeaders(),
                                        errorBody.getBytes(),
                                        null
                                ));
                            })
                )
                .bodyToMono(JsonNode.class);
    }

    public Flux<String> chatStreamRaw(JsonNode body, Map<String, String> headers) {
        String bodyStr = bodyToString(body);
        log.info("=== [LLM] Stream Request to {}. Body: {}", chatPath, bodyStr);

        return webClient.post()
                .uri(chatPath)
                .headers(h -> applyHeaders(h, headers))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(bodyStr)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> 
                    response.bodyToMono(String.class)
                            .flatMap(errorBody -> {
                                log.error("=== [LLM] Stream Error {}: {}", response.statusCode(), errorBody);
                                return Mono.error(new WebClientResponseException(
                                        response.statusCode().value(),
                                        "LLM Stream Error: " + errorBody,
                                        response.headers().asHttpHeaders(),
                                        errorBody.getBytes(),
                                        null
                                ));
                            })
                )
                .bodyToFlux(String.class);
    }

    public String defaultModel() {
        return defaultModel == null ? "default" : defaultModel;
    }

    void applyHeaders(HttpHeaders target, Map<String, String> headers) {
        if (apiKey != null && target.getFirst(HttpHeaders.AUTHORIZATION) == null) {
            target.setBearerAuth(apiKey);
        }
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                if (apiKey != null
                        && ("X-AI-API-KEY".equalsIgnoreCase(key)
                        || HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key))) {
                    return;
                }
                target.set(key, value);
            }
        });
    }

    private String bodyToString(JsonNode body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return body.toString();
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
