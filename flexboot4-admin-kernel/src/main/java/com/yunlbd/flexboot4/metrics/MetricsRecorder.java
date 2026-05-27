package com.yunlbd.flexboot4.metrics;

import java.time.Duration;
import java.util.Map;

public interface MetricsRecorder {

    void increment(String name, Map<String, String> tags);

    void recordDuration(String name, Duration duration, Map<String, String> tags);

    default void increment(String name) {
        increment(name, Map.of());
    }
}
