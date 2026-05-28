package com.yunlbd.flexboot4.excel.sys;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysUserImportRow {
    @ExcelProperty("登录名")
    private String username;
    @ExcelProperty("用户名")
    private String realName;
    @ExcelProperty("邮箱")
    private String email;
    @ExcelProperty("手机")
    private String phone;
    @ExcelProperty("性别")
    private String gender;
    @ExcelProperty("部门ID")
    private String deptId;
    @ExcelProperty("状态")
    private Integer status;
    @ExcelProperty("备注")
    private String remark;
}
