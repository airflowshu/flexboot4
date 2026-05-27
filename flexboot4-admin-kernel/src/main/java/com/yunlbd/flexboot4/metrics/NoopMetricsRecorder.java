package com.yunlbd.flexboot4.metrics;

import java.time.Duration;
import java.util.Map;

public class NoopMetricsRecorder implements MetricsRecorder {

    @Override
    public void increment(String name, Map<String, String> tags) {
        // Intentionally empty.
    }

    @Override
    public void recordDuration(String name, Duration duration, Map<String, String> tags) {
        // Intentionally empty.
    }
}
