package com.yunlbd.flexboot4.file.parse;

import com.yunlbd.flexboot4.file.parse.impl.DocxFileParser;
import com.yunlbd.flexboot4.file.parse.impl.PdfFileParser;
import com.yunlbd.flexboot4.file.parse.impl.TxtFileParser;
import com.yunlbd.flexboot4.file.parse.impl.XlsxFileParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileParserSmokeTest {

    @Test
    void txtParserShouldWork() {
        TxtFileParser p = new TxtFileParser();
        ParsedDocument doc = p.parse("f1", new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));
        assertTrue(doc.fullText().contains("hello"));
        assertFalse(doc.blocks().isEmpty());
    }

    @Test
    void docxParserShouldWork() throws Exception {
        DocxFileParser p = new DocxFileParser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XWPFDocument d = new XWPFDocument()) {
            XWPFParagraph h1 = d.createParagraph();
            h1.setStyle("Heading1");
            h1.createRun().setText("H1");
            XWPFParagraph para = d.createParagraph();
            para.createRun().setText("docx-text");
            d.write(out);
        }
        ParsedDocument doc = p.parse("f2", new ByteArrayInputStream(out.toByteArray()));
        assertTrue(doc.fullText().contains("docx-text"));
        assertTrue(doc.blocks().stream().anyMatch(b -> "H1".equals(b.sectionTitle())));
    }

    @Test
    void xlsxParserShouldWork() throws Exception {
        XlsxFileParser p = new XlsxFileParser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("s1");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("xlsx-text");
            wb.write(out);
        }
        ParsedDocument doc = p.parse("f3", new ByteArrayInputStream(out.toByteArray()));
        assertTrue(doc.fullText().contains("xlsx-text"));
        assertTrue(doc.blocks().stream().anyMatch(b -> "s1".equals(b.sectionTitle())));
    }

    @Test
    void pdfParserShouldWork() throws Exception {
        PdfFileParser p = new PdfFileParser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText("pdf-text");
                cs.endText();
            }
            doc.save(out);
        }
        ParsedDocument parsed = p.parse("f4", new ByteArrayInputStream(out.toByteArray()));
        assertTrue(parsed.fullText().contains("pdf-text"));
        assertTrue(parsed.blocks().stream().anyMatch(b -> b.pageNumber() != null && b.pageNumber() == 1));
    }
}
