package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_menu")
public class SysRoleMenu extends BaseEntity {
    private String roleId;
    private String menuId;
}
