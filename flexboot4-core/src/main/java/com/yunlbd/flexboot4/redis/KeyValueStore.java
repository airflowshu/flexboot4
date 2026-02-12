package com.yunlbd.flexboot4.redis;

import java.time.Duration;
import java.util.Optional;

public interface KeyValueStore {
    Optional<String> get(String key);

    void set(String key, String value, Duration ttl);

    void delete(String key);
}

