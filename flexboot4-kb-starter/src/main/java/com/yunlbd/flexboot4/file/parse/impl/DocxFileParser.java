package com.yunlbd.flexboot4.file.parse.impl;

import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DocxFileParser implements FileParser {

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null && contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return true;
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        return lower.endsWith(".docx");
    }

    @Override
    public ParsedDocument parse(String fileId, InputStream in) {
        try (XWPFDocument doc = new XWPFDocument(in)) {
            List<ParsedBlock> blocks = new ArrayList<>();
            StringBuilder full = new StringBuilder();
            String currentSectionTitle = null;

            for (IBodyElement el : doc.getBodyElements()) {
                if (el instanceof XWPFParagraph p) {
                    String t = p.getText();
                    if (t == null) {
                        continue;
                    }
                    String text = t.trim();
                    if (text.isBlank()) {
                        continue;
                    }
                    if (isHeading(p)) {
                        currentSectionTitle = text;
                        appendParagraph(full, text);
                        continue;
                    }
                    appendParagraph(full, text);
                    blocks.add(new ParsedBlock(text, null, currentSectionTitle));
                    continue;
                }
                if (el instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        StringBuilder line = new StringBuilder();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String c = cell.getText();
                            String ct = c == null ? "" : c.trim();
                            if (!ct.isBlank()) {
                                if (!line.isEmpty()) {
                                    line.append('\t');
                                }
                                line.append(ct);
                            }
                        }
                        if (!line.isEmpty()) {
                            String rowText = line.toString();
                            appendParagraph(full, rowText);
                            blocks.add(new ParsedBlock(rowText, null, currentSectionTitle));
                        }
                    }
                }
            }

            return new ParsedDocument(fileId, full.toString().trim(), 0, Map.of("type", "docx"), blocks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isHeading(XWPFParagraph p) {
        String style = p.getStyle();
        if (style == null || style.isBlank()) {
            return false;
        }
        String lower = style.toLowerCase();
        return lower.contains("heading") || lower.contains("title") || style.contains("标题") || style.contains("题注");
    }

    private void appendParagraph(StringBuilder sb, String text) {
        if (sb.isEmpty()) {
            sb.append(text);
            return;
        }
        sb.append("\n\n").append(text);
    }
}
