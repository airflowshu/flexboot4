package com.yunlbd.flexboot4.lock;

import java.time.Duration;

public class NoopDistributedLockService implements DistributedLockService {

    @Override
    public boolean tryLock(String name, Duration ttl) {
        return true;
    }

    @Override
    public void unlock(String name) {
        // Local/noop fallback.
    }
}
