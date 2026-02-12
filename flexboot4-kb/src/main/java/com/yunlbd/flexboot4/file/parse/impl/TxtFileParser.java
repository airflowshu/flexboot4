package com.yunlbd.flexboot4.file.parse.impl;

import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TxtFileParser implements FileParser {

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().startsWith("text/")) {
            return true;
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".log");
    }

    @Override
    public ParsedDocument parse(String fileId, InputStream in) {
        try {
            byte[] bytes = readAllBytes(in, 8 * 1024 * 1024);
            Charset cs = detectCharset(bytes);
            String text = new String(bytes, cs);
            List<ParsedBlock> blocks = parseBlocks(text);
            return new ParsedDocument(fileId, text, 1, Map.of("charset", cs.name(), "type", "text"), blocks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ParsedBlock> parseBlocks(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");
        List<ParsedBlock> blocks = new ArrayList<>();
        String currentTitle = null;
        StringBuilder para = new StringBuilder();

        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.startsWith("#")) {
                flushParagraph(blocks, para, currentTitle);
                currentTitle = line.replaceFirst("^#+\\s*", "").trim();
                continue;
            }
            if (line.isBlank()) {
                flushParagraph(blocks, para, currentTitle);
                continue;
            }
            if (!para.isEmpty()) {
                para.append('\n');
            }
            para.append(line);
        }
        flushParagraph(blocks, para, currentTitle);
        return blocks;
    }

    private void flushParagraph(List<ParsedBlock> blocks, StringBuilder para, String title) {
        if (para.isEmpty()) {
            return;
        }
        blocks.add(new ParsedBlock(para.toString(), null, title));
        para.setLength(0);
    }

    private Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }

    private byte[] readAllBytes(InputStream in, int maxBytes) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("file too large for txt parser");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
