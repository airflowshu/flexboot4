package com.yunlbd.flexboot4.config;

import com.yunlbd.flexboot4.file.parse.ChunkingOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file.parse")
public record FileParseProperties(
        int chunkTokens,
        int overlapTokens,
        int maxChunks
) {
    public ChunkingOptions toChunkingOptions() {
        int ct = chunkTokens > 0 ? chunkTokens : 700;
        int ot = overlapTokens >= 0 ? overlapTokens : 80;
        int mc = maxChunks > 0 ? maxChunks : 2000;
        return new ChunkingOptions(ct, ot, mc);
    }
}

