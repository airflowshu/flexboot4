package com.yunlbd.flexboot4.aigateway.web;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class SseController {
    @GetMapping(value = "/api/ai/sse/time", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> time() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> ServerSentEvent.<String>builder()
                        .event("time")
                        .data(String.valueOf(System.currentTimeMillis()))
                        .build());
    }
}

