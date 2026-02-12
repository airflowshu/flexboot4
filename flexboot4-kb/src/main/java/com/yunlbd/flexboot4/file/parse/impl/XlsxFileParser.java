package com.yunlbd.flexboot4.file.parse.impl;

import com.yunlbd.flexboot4.file.parse.FileParser;
import com.yunlbd.flexboot4.file.parse.ParsedBlock;
import com.yunlbd.flexboot4.file.parse.ParsedDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class XlsxFileParser implements FileParser {

    private final DataFormatter formatter = new DataFormatter();

    @Override
    public boolean supports(String contentType, String fileName) {
        if (contentType != null && contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            return true;
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        return lower.endsWith(".xlsx");
    }

    @Override
    public ParsedDocument parse(String fileId, InputStream in) {
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            StringBuilder sb = new StringBuilder();
            List<ParsedBlock> blocks = new ArrayList<>();
            int sheets = wb.getNumberOfSheets();
            for (int s = 0; s < sheets; s++) {
                Sheet sheet = wb.getSheetAt(s);
                if (sheet == null) {
                    continue;
                }
                String sheetName = sheet.getSheetName();
                sb.append("# ").append(sheetName).append('\n');
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        String v = formatter.formatCellValue(cell);
                        if (v != null) {
                            if (!line.isEmpty()) {
                                line.append('\t');
                            }
                            line.append(v.trim());
                        }
                    }
                    if (!line.isEmpty()) {
                        String rowText = line.toString();
                        sb.append(rowText).append('\n');
                        blocks.add(new ParsedBlock(rowText, null, sheetName));
                    }
                }
                sb.append('\n');
            }
            return new ParsedDocument(fileId, sb.toString(), 0, Map.of("type", "xlsx", "sheetCount", sheets), blocks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
