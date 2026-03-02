package com.yunlbd.flexboot4.file.parse;

import java.util.List;
import java.util.Map;

public record ParsedDocument(
        String fileId,
        String fullText,
        int pageCount,
        Map<String, Object> metadata,
        List<ParsedBlock> blocks
) {
    public ParsedDocument(String fileId, String fullText, int pageCount, Map<String, Object> metadata) {
        this(fileId, fullText, pageCount, metadata, fullText == null ? List.of() : List.of(new ParsedBlock(fullText, null, null)));
    }
}
