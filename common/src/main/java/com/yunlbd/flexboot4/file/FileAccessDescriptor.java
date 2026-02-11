package com.yunlbd.flexboot4.file;

import java.time.Instant;

public record FileAccessDescriptor(
        String url,
        Instant expireAt,
        String disposition,
        String token
) {
}

