package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysRoleCreateReq {
    @NotBlank
    private String roleName;
    @NotBlank
    private String roleValue;
    private Integer status;
    private String description;
    private Integer orderNo;
    private String remark;
}
