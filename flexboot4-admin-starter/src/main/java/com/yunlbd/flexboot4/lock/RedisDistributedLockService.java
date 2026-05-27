package com.yunlbd.flexboot4.lock;

import com.yunlbd.flexboot4.metrics.MetricsRecorder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class RedisDistributedLockService implements DistributedLockService {

    private final StringRedisTemplate redisTemplate;
    private final LockProperties properties;
    private final MetricsRecorder metricsRecorder;
    private final String ownerId = UUID.randomUUID().toString();

    public RedisDistributedLockService(StringRedisTemplate redisTemplate,
                                       LockProperties properties,
                                       MetricsRecorder metricsRecorder) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.metricsRecorder = metricsRecorder;
    }

    @Override
    public boolean tryLock(String name, Duration ttl) {
        Duration lease = ttl == null || ttl.isNegative() || ttl.isZero()
                ? Duration.ofMillis(defaultTtlMillis())
                : ttl;
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key(name), ownerId, lease);
        metricsRecorder.increment("flexboot4.lock.acquire", Map.of(
                "name", name,
                "result", Boolean.TRUE.equals(acquired) ? "success" : "rejected"
        ));
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(String name) {
        String key = key(name);
        String currentOwner = redisTemplate.opsForValue().get(key);
        if (ownerId.equals(currentOwner)) {
            redisTemplate.delete(key);
            metricsRecorder.increment("flexboot4.lock.release", Map.of("name", name, "result", "released"));
        } else {
            metricsRecorder.increment("flexboot4.lock.release", Map.of("name", name, "result", "foreign_owner"));
        }
    }

    private String key(String name) {
        String prefix = properties.keyPrefix();
        String actualPrefix = prefix == null || prefix.isBlank() ? "flexboot4:lock:" : prefix;
        return actualPrefix + name;
    }

    private long defaultTtlMillis() {
        return properties.defaultTtlMillis() > 0 ? properties.defaultTtlMillis() : 60000L;
    }
}
