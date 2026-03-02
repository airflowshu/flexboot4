package com.yunlbd.flexboot4.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

public class RedisTableVersionProvider implements TableVersionProvider {
    private final StringRedisTemplate redis;
    private final String keyPrefix;

    public RedisTableVersionProvider(StringRedisTemplate redis, String keyPrefix) {
        this.redis = redis;
        this.keyPrefix = keyPrefix == null ? "cache:ver:" : keyPrefix;
    }

    @Override
    public long getVersion(String table) {
        if (table == null || table.isBlank()) {
            return 0L;
        }
        assert redis.getConnectionFactory() != null;
        RedisAtomicLong counter = new RedisAtomicLong(keyPrefix + table, redis.getConnectionFactory());
        return counter.get();
    }

    @Override
    public long bumpVersion(String table) {
        if (table == null || table.isBlank()) {
            return 0L;
        }
        assert redis.getConnectionFactory() != null;
        RedisAtomicLong counter = new RedisAtomicLong(keyPrefix + table, redis.getConnectionFactory());
        return counter.incrementAndGet();
    }
}

