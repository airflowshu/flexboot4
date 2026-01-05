package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleValue; // e.g., 'super', 'admin','user','test'
    private Integer status; // 1: enabled, 0: disabled
    private String description;
    private Integer orderNo;
}
