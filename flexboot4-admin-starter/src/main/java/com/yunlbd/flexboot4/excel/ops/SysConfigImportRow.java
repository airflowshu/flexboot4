package com.yunlbd.flexboot4.excel.ops;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysConfigImportRow {
    @ExcelProperty("配置键")
    private String configKey;
    @ExcelProperty("配置值")
    private String configValue;
    @ExcelProperty("配置类型")
    private String configType;
    @ExcelProperty("配置描述")
    private String description;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("备注")
    private String remark;
}
