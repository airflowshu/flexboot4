package com.yunlbd.flexboot4.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("sys_role")
public class SysRole extends BaseEntity {

    private String roleName;
    private String roleValue; // e.g., 'super', 'admin','user','test'
    private Integer status; // 1: enabled, 0: disabled
    private String description;
    private Integer orderNo;

    @RelationManyToMany(
            joinTable = "sys_role_menu",
            selfField = "id", joinSelfColumn = "role_id",
            targetField = "id", joinTargetColumn = "menu_id"
    )
    private List<SysMenu> menus;

    @JsonIgnore
    @RelationManyToMany(
            joinTable = "sys_user_role",
            selfField = "id", joinSelfColumn = "role_id",
            targetField = "id", joinTargetColumn = "user_id"
    )
    private List<SysUser> users;
}
