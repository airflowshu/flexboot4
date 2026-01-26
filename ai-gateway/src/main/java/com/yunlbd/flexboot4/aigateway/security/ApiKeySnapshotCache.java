package com.yunlbd.flexboot4.aigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunlbd.flexboot4.apikey.ApiKeyRule;
import com.yunlbd.flexboot4.apikey.ApiKeySnapshot;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ApiKeySnapshotCache {

    private static final String VERSION_KEY = "cache:ver:ai_api_key";
    private static final String SNAPSHOT_PREFIX = "aikey:snapshot:ver:";
    private static final String SNAPSHOT_LATEST = "aikey:snapshot:latest";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicReference<State> stateRef = new AtomicReference<>(State.empty());

    public ApiKeySnapshotCache(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        refresh().subscribe();
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 300000)
    public void scheduledRefresh() {
        refresh().subscribe();
    }

    public Mono<List<ApiKeyRule>> activeRulesForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return Mono.just(List.of());
        }
        return ensureReady()
                .map(state -> {
                    List<ApiKeyRule> rules = state.byUserId.getOrDefault(userId, List.of());
                    if (rules.isEmpty()) {
                        return List.of();
                    }
                    List<ApiKeyRule> active = new ArrayList<>();
                    for (ApiKeyRule r : rules) {
                        if (r != null && r.status() == 1) {
                            active.add(r);
                        }
                    }
                    return active;
                });
    }

    public Mono<Void> refresh() {
        return readVersion()
                .flatMap(this::loadSnapshotByVersion)
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<State> ensureReady() {
        State st = stateRef.get();
        if (st.version >= 0) {
            return Mono.just(st);
        }
        return refresh().then(Mono.fromSupplier(stateRef::get));
    }

    private Mono<Long> readVersion() {
        return redisTemplate.opsForValue()
                .get(VERSION_KEY)
                .flatMap(v -> {
                    try {
                        return Mono.just(Long.parseLong(v));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .switchIfEmpty(redisTemplate.opsForValue().get(SNAPSHOT_LATEST).flatMap(v -> {
                    try {
                        return Mono.just(Long.parseLong(v));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                }))
                .defaultIfEmpty(0L);
    }

    private Mono<Void> loadSnapshotByVersion(long version) {
        State current = stateRef.get();
        if (current.version == version) {
            return Mono.empty();
        }
        String key = SNAPSHOT_PREFIX + version;
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    if (json == null || json.isBlank()) {
                        return Mono.empty();
                    }
                    try {
                        ApiKeySnapshot snapshot = objectMapper.readValue(json, ApiKeySnapshot.class);
                        if (snapshot == null) {
                            return Mono.empty();
                        }
                        stateRef.set(State.from(snapshot));
                        return Mono.empty();
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                });
    }

    private static final class State {
        private final long version;
        private final Map<String, List<ApiKeyRule>> byUserId;

        private State(long version, Map<String, List<ApiKeyRule>> byUserId) {
            this.version = version;
            this.byUserId = byUserId;
        }

        static State empty() {
            return new State(-1L, Map.of());
        }

        static State from(ApiKeySnapshot snapshot) {
            if (snapshot.keys() == null || snapshot.keys().isEmpty()) {
                return new State(snapshot.version(), Map.of());
            }
            Map<String, List<ApiKeyRule>> byUser = new HashMap<>();
            for (ApiKeyRule r : snapshot.keys()) {
                if (r == null) {
                    continue;
                }
                String userId = r.userId();
                if (userId == null || userId.isBlank()) {
                    continue;
                }
                byUser.computeIfAbsent(userId, k -> new ArrayList<>()).add(r);
            }
            Map<String, List<ApiKeyRule>> frozen = new HashMap<>();
            for (Map.Entry<String, List<ApiKeyRule>> e : byUser.entrySet()) {
                frozen.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
            }
            return new State(snapshot.version(), Collections.unmodifiableMap(frozen));
        }
    }
}

