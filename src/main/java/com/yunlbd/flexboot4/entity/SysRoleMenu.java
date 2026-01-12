package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_role_menu")
@Schema(name = "SysRoleMenu")
public class SysRoleMenu extends BaseEntity {
    private String roleId;
    private String menuId;
}
