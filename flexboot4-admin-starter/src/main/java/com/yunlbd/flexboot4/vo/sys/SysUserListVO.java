package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserListVO extends BaseAuditVO {
    private String username;
    private String realName;
    private String profileFileId;
    private String avatar;
    private String email;
    private String phone;
    private String gender;
    private String genderStr;
    private String deptId;
    private Integer status;
    private SysDeptSimpleVO dept;
    private List<SysRoleSimpleVO> roles;
}
