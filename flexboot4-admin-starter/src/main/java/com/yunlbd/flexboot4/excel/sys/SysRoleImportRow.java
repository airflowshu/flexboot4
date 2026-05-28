package com.yunlbd.flexboot4.excel.sys;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysRoleImportRow {
    @ExcelProperty("角色名")
    private String roleName;
    @ExcelProperty("角色值")
    private String roleValue;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("描述")
    private String description;
    @ExcelProperty("排序")
    private Integer orderNo;
    @ExcelProperty("备注")
    private String remark;
}
