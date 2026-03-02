package com.yunlbd.flexboot4.redis;

import java.time.Duration;

public interface TokenBlacklistStore {
    void blacklist(String tokenId, Duration ttl);

    boolean isBlacklisted(String tokenId);
}

