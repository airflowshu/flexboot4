package com.yunlbd.flexboot4.aigateway.rag.embedding.strategy;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文档类型枚举 - 用于路由到不同的 Embedding 模型
 */
public enum DocumentType {
    TEXT("text", "通用文本", "bge-m3", 1024),
    CODE("code", "代码", "codegemma", 2048),
    TABLE("table", "表格", "bge-m3", 1024),
    MARKDOWN("markdown", "Markdown文档", "bge-m3", 1024),
    PDF("pdf", "PDF文档", "bge-m3", 1024),
    EXCEL("excel", "Excel文档", "bge-m3", 1024),
    CSV("csv", "CSV文档", "bge-m3", 1024),
    WORD("word", "Word文档", "bge-m3", 1024),
    ;

    private final String code;
    private final String description;
    private final String defaultModel;
    private final int defaultDimension;

    private static final Map<String, DocumentType> BY_EXTENSION;
    private static final Pattern CODE_EXTENSIONS = Pattern.compile(
            "\\.(java|py|js|ts|go|rs|cpp|c|h|hpp|php|rb|swift|kt|scala|sql|sh|bash|ps1)$",
            Pattern.CASE_INSENSITIVE
    );

    static {
        BY_EXTENSION = Map.of(
                ".md", MARKDOWN,
                ".pdf", PDF,
                ".doc", WORD,
                ".docx", WORD,
                ".xlsx", EXCEL,
                ".xls", EXCEL,
                ".csv", CSV
        );
    }

    DocumentType(String code, String description, String defaultModel, int defaultDimension) {
        this.code = code;
        this.description = description;
        this.defaultModel = defaultModel;
        this.defaultDimension = defaultDimension;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public int getDefaultDimension() {
        return defaultDimension;
    }

    /**
     * 根据文件名推断文档类型
     */
    public static DocumentType fromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return TEXT;
        }

        String lower = fileName.toLowerCase();

        // 先检查代码文件
        if (CODE_EXTENSIONS.matcher(lower).find()) {
            return CODE;
        }

        // 根据扩展名判断
        int lastDot = lower.lastIndexOf('.');
        if (lastDot > 0) {
            String ext = lower.substring(lastDot);
            DocumentType type = BY_EXTENSION.get(ext);
            if (type != null) {
                return type;
            }
        }

        return TEXT;
    }

    /**
     * 根据文件 ID 推断文档类型（从 ID 中提取文件名特征）
     */
    public static DocumentType fromFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return TEXT;
        }
        // fileId 通常包含原始文件名
        return fromFileName(fileId);
    }

    /**
     * 根据 MIME 类型推断文档类型
     */
    public static DocumentType fromMimeType(String mimeType) {
        if (mimeType == null) {
            return TEXT;
        }
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> PDF;
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                 "application/vnd.ms-excel" -> EXCEL;
            case "application/vnd.ms-word",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> WORD;
            case "text/csv" -> CSV;
            case "text/markdown" -> MARKDOWN;
            case "application/json", "text/plain", "text/html", "text/xml" -> TEXT;
            default -> TEXT;
        };
    }

    @Override
    public String toString() {
        return "%s(%s)".formatted(description, code);
    }
}
