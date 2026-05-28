package com.yunlbd.flexboot4.excel.sys;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysMenuExportRow {
    @ExcelProperty("父菜单ID")
    private String parentId;
    @ExcelProperty("路径")
    private String path;
    @ExcelProperty("名称")
    private String name;
    @ExcelProperty("组件")
    private String component;
    @ExcelProperty("标题")
    private String title;
    @ExcelProperty("图标")
    private String icon;
    @ExcelProperty("类型")
    private String type;
    @ExcelProperty("权限码")
    private String authCode;
    @ExcelProperty("排序")
    private Integer orderNo;
    @ExcelProperty("状态")
    private Integer status;
}
