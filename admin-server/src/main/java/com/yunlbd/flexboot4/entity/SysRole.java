package com.yunlbd.flexboot4.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "SysRole")
public class SysRole extends BaseEntity {

    @Schema(title = "角色名")
    private String roleName;
    @Schema(title = "角色值")
    private String roleValue; // e.g., 'super', 'admin','user','test'
    @Schema(title = "角色状态")
    private Integer status; // 1: enabled, 0: disabled
    @Schema(title = "角色描述")
    private String description;
    @Schema(title = "排序号")
    private Integer orderNo;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "菜单按钮集合") //只在入参中隐藏（响应中可见）
    @RelationManyToMany(
            joinTable = "sys_role_menu",
            selfField = "id", joinSelfColumn = "role_id",
            targetField = "id", joinTargetColumn = "menu_id"
    )
    private List<SysMenu> menus;

    @JsonIgnore
    @Schema(hidden = true)
    @RelationManyToMany(
            joinTable = "sys_user_role",
            selfField = "id", joinSelfColumn = "role_id",
            targetField = "id", joinTargetColumn = "user_id"
    )
    private List<SysUser> users;
}
