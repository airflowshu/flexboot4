package com.yunlbd.flexboot4.file.parse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockChunkerTest {

    @Test
    void chunkBlocksShouldPreservePageAndTitle() {
        List<ParsedBlock> blocks = List.of(
                new ParsedBlock("A ".repeat(1000), 1, "Title-A"),
                new ParsedBlock("B ".repeat(1000), 2, "Title-B")
        );
        List<ChunkedText> chunks = BlockChunker.chunkBlocks(blocks, new ChunkingOptions(50, 10, 100));
        assertTrue(chunks.stream().anyMatch(c -> c.pageNumber() != null && c.pageNumber() == 1 && "Title-A".equals(c.sectionTitle())));
        assertTrue(chunks.stream().anyMatch(c -> c.pageNumber() != null && c.pageNumber() == 2 && "Title-B".equals(c.sectionTitle())));
    }
}

