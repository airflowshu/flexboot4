package com.yunlbd.flexboot4.excel.sys;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysFileExportRow {
    @ExcelProperty("租户ID")
    private String tenantId;
    @ExcelProperty("业务类型")
    private String bizType;
    @ExcelProperty("业务ID")
    private String bizId;
    @ExcelProperty("文件名")
    private String fileName;
    @ExcelProperty("扩展名")
    private String fileExt;
    @ExcelProperty("MIME类型")
    private String mimeType;
    @ExcelProperty("文件大小")
    private Long fileSize;
    @ExcelProperty("存储类型")
    private String storageType;
    @ExcelProperty("存储桶")
    private String bucketName;
    @ExcelProperty("对象Key")
    private String objectKey;
    @ExcelProperty("AI状态")
    private String aiStatus;
}
