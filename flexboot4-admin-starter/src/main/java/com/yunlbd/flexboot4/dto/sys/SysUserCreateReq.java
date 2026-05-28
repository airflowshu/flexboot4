package com.yunlbd.flexboot4.dto.sys;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysUserCreateReq {
    @NotBlank
    private String username;
    @NotBlank
    private String realName;
    private String profileFileId;
    private String email;
    private String phone;
    private String gender;
    private String deptId;
    private Integer status;
    private String remark;
}
