package com.yunlbd.flexboot4.dto.sys;

import lombok.Data;

@Data
public class SysRoleUpdateReq {
    private String roleName;
    private String roleValue;
    private Integer status;
    private String description;
    private Integer orderNo;
    private String remark;
}
