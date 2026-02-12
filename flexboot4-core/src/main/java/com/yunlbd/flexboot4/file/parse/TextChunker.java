package com.yunlbd.flexboot4.file.parse;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String text, ChunkingOptions opts) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ChunkingOptions o = opts != null ? opts : ChunkingOptions.defaults();
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] paras = normalized.split("\\n{2,}");
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentTokens = 0;

        for (String raw : paras) {
            String p = raw == null ? "" : raw.trim();
            if (p.isEmpty()) {
                continue;
            }
            int pt = TokenEstimator.estimateTokens(p);
            if (pt > o.chunkTokens()) {
                for (String part : splitLongParagraph(p, o.chunkTokens())) {
                    addChunk(out, current, part, o);
                }
                currentTokens = TokenEstimator.estimateTokens(current.toString());
                if (out.size() >= o.maxChunks()) {
                    break;
                }
                continue;
            }
            if (currentTokens + pt > o.chunkTokens()) {
                flush(out, current);
                currentTokens = 0;
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(p);
            currentTokens += pt;
            if (out.size() >= o.maxChunks()) {
                break;
            }
        }
        flush(out, current);

        if (o.overlapTokens() > 0 && out.size() > 1) {
            List<String> withOverlap = new ArrayList<>(out.size());
            String prev = null;
            for (String c : out) {
                if (prev == null) {
                    withOverlap.add(c);
                    prev = c;
                    continue;
                }
                String overlap = tailByApproxTokens(prev, o.overlapTokens());
                if (!overlap.isBlank()) {
                    withOverlap.add(overlap + "\n" + c);
                } else {
                    withOverlap.add(c);
                }
                prev = c;
            }
            return withOverlap;
        }
        return out;
    }

    private static void addChunk(List<String> out, StringBuilder current, String part, ChunkingOptions opts) {
        int pt = TokenEstimator.estimateTokens(part);
        int ct = TokenEstimator.estimateTokens(current.toString());
        if (ct + pt > opts.chunkTokens()) {
            flush(out, current);
        }
        if (!current.isEmpty()) {
            current.append("\n\n");
        }
        current.append(part);
    }

    private static void flush(List<String> out, StringBuilder current) {
        if (!current.isEmpty()) {
            out.add(current.toString());
            current.setLength(0);
        }
    }

    private static List<String> splitLongParagraph(String p, int chunkTokens) {
        List<String> parts = new ArrayList<>();
        String[] sentences = p.split("(?<=[。！？.!?])");
        StringBuilder sb = new StringBuilder();
        int tokens = 0;
        for (String s : sentences) {
            String t = s == null ? "" : s.trim();
            if (t.isEmpty()) {
                continue;
            }
            int st = TokenEstimator.estimateTokens(t);
            if (tokens + st > chunkTokens && !sb.isEmpty()) {
                parts.add(sb.toString());
                sb.setLength(0);
                tokens = 0;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(t);
            tokens += st;
        }
        if (!sb.isEmpty()) {
            parts.add(sb.toString());
        }
        return parts.isEmpty() ? List.of(p) : parts;
    }

    private static String tailByApproxTokens(String text, int tokens) {
        if (text == null || text.isBlank() || tokens <= 0) {
            return "";
        }
        int estimated = TokenEstimator.estimateTokens(text);
        if (estimated <= tokens) {
            return text;
        }
        int len = text.length();
        int approxChars = Math.min(len, tokens * 4);
        return text.substring(Math.max(0, len - approxChars));
    }
}

