package com.yunlbd.flexboot4.dto.sys;

import lombok.Data;

@Data
public class SysUserUpdateReq {
    private String username;
    private String realName;
    private String profileFileId;
    private String email;
    private String phone;
    private String gender;
    private String deptId;
    private Integer status;
    private String remark;
}
