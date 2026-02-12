package com.yunlbd.flexboot4.file.parse;

import java.util.ArrayList;
import java.util.List;

public final class BlockChunker {

    private BlockChunker() {
    }

    public static List<ChunkedText> chunkBlocks(List<ParsedBlock> blocks, ChunkingOptions opts) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        ChunkingOptions o = opts != null ? opts : ChunkingOptions.defaults();

        List<ChunkedText> out = new ArrayList<>();
        for (ParsedBlock b : blocks) {
            if (b == null || b.text() == null || b.text().isBlank()) {
                continue;
            }
            int remaining = o.maxChunks() - out.size();
            if (remaining <= 0) {
                break;
            }
            ChunkingOptions perBlock = new ChunkingOptions(o.chunkTokens(), o.overlapTokens(), remaining);
            List<String> pieces = TextChunker.chunk(b.text(), perBlock);
            for (String p : pieces) {
                if (p == null || p.isBlank()) {
                    continue;
                }
                out.add(new ChunkedText(p, b.pageNumber(), b.sectionTitle()));
                if (out.size() >= o.maxChunks()) {
                    break;
                }
            }
            if (out.size() >= o.maxChunks()) {
                break;
            }
        }
        return out;
    }
}

