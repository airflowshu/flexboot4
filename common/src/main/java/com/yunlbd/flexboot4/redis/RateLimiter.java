package com.yunlbd.flexboot4.redis;

import java.time.Duration;

public interface RateLimiter {
    boolean tryAcquire(String key, long permits, Duration window);
}

