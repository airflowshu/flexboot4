package com.yunlbd.flexboot4.file.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void chunkShouldSplitText() {
        String text = ("段落一。" + "a".repeat(2000) + "\n\n段落二。" + "b".repeat(2000) + "\n\n段落三。").repeat(2);
        List<String> chunks = TextChunker.chunk(text, new ChunkingOptions(200, 20, 100));
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
    }
}

