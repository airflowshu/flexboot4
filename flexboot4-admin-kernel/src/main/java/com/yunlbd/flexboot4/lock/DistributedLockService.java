package com.yunlbd.flexboot4.lock;

import java.time.Duration;
import java.util.function.Supplier;

public interface DistributedLockService {

    boolean tryLock(String name, Duration ttl);

    void unlock(String name);

    default boolean executeIfLocked(String name, Duration ttl, Runnable action) {
        return executeIfLocked(name, ttl, () -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    default <T> boolean executeIfLocked(String name, Duration ttl, Supplier<T> action) {
        if (!tryLock(name, ttl)) {
            return false;
        }
        try {
            action.get();
            return true;
        } finally {
            unlock(name);
        }
    }
}
