package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_user_role")
public class SysUserRole extends BaseEntity {
    private Long userId;
    private Long roleId;
}
