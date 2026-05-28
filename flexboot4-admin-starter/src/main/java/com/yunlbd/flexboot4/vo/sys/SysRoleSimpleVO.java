package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;

@Data
public class SysRoleSimpleVO {
    private String id;
    private String roleName;
    private String roleValue;
    private Integer status;
    private Integer orderNo;
    private String description;
    private String remark;
}
