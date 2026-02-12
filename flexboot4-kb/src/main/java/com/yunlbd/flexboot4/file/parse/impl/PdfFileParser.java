package com.yunlbd.flexboot4.file.parse.impl;

import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class PdfFileParser implements FileParser {

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return true;
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        return lower.endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(String fileId, InputStream in) {
        try (RandomAccessReadBuffer ra = new RandomAccessReadBuffer(in);
             PDDocument doc = Loader.loadPDF(ra)) {
            int pages = doc.getNumberOfPages();
            TreeMap<Integer, String> outlineTitles = extractOutlineTitles(doc);
            List<ParsedBlock> blocks = new ArrayList<>();
            StringBuilder full = new StringBuilder();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(doc);
                String text = pageText == null ? "" : pageText.trim();
                if (text.isBlank()) {
                    continue;
                }
                String title = null;
                var floor = outlineTitles.floorEntry(page);
                if (floor != null) {
                    title = floor.getValue();
                }
                blocks.add(new ParsedBlock(text, page, title));
                if (!full.isEmpty()) {
                    full.append("\n\n");
                }
                full.append(text);
            }

            return new ParsedDocument(fileId, full.toString(), pages, Map.of("type", "pdf", "pageCount", pages), blocks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TreeMap<Integer, String> extractOutlineTitles(PDDocument doc) {
        TreeMap<Integer, String> map = new TreeMap<>();
        try {
            if (doc.getDocumentCatalog() == null) {
                return map;
            }
            PDDocumentOutline outline = doc.getDocumentCatalog().getDocumentOutline();
            if (outline == null) {
                return map;
            }
            PDOutlineItem item = outline.getFirstChild();
            while (item != null) {
                collectOutlineItem(doc, item, map);
                item = item.getNextSibling();
            }
        } catch (Exception ignored) {
            return map;
        }
        return map;
    }

    private void collectOutlineItem(PDDocument doc, PDOutlineItem item, TreeMap<Integer, String> map) {
        if (item == null) {
            return;
        }
        try {
            String title = item.getTitle();
            var page = item.findDestinationPage(doc);
            if (title != null && !title.isBlank() && page != null) {
                int pageNumber = doc.getPages().indexOf(page) + 1;
                if (pageNumber > 0) {
                    map.putIfAbsent(pageNumber, title.trim());
                }
            }
        } catch (Exception ignored) {
        }
        PDOutlineItem child = item.getFirstChild();
        while (child != null) {
            collectOutlineItem(doc, child, map);
            child = child.getNextSibling();
        }
    }
}
