package com.yunlbd.flexboot4.excel.ops;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiApiKeyImportRow {
    @ExcelProperty("名称")
    private String keyName;
    @ExcelProperty("用户ID")
    private String userId;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("额度")
    private Long quote;
    @ExcelProperty("模型范围")
    private String modelScope;
    @ExcelProperty("过期时间")
    private LocalDateTime expiresAt;
    @ExcelProperty("说明")
    private String notes;
}
