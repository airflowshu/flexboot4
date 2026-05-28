package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleListVO extends BaseAuditVO {
    private String roleName;
    private String roleValue;
    private Integer status;
    private String description;
    private Integer orderNo;
}
