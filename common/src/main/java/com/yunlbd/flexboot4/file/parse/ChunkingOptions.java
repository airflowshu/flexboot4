package com.yunlbd.flexboot4.file.parse;

public record ChunkingOptions(
        int chunkTokens,
        int overlapTokens,
        int maxChunks
) {
    public static ChunkingOptions defaults() {
        return new ChunkingOptions(700, 80, 2000);
    }
}

