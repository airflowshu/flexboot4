package com.yunlbd.flexboot4.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.*;
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
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    private String parentId;
    private String path;
    private String name; // Router name
    private String component; // Component path
    private String redirect;
    
    // Meta fields
    private String title;
    private String icon;
    private Integer orderNo;
    private Boolean hideMenu;
    private Boolean keepAlive;
    
    // New fields for Vben Admin
    private String activeIcon;
    private String badge;
    private String badgeType; // dot, normal
    private String badgeVariants; // primary, error, success, warning
    private String link;
    private String iframeSrc;
    private Boolean affixTab;
    private Boolean hideChildrenInMenu;
    private Boolean hideBreadcrumb;
    private Boolean hideTab;
    private Boolean menuVisibleWithForbidden; // 403 visible
    private String authority; // Role list, comma separated

    private String permission; // Permission code e.g., 'sys:user:add'
    private String type; //catalog menu button embedded link
    private Integer status;

    @RelationManyToOne(selfField = "parentId", targetField = "id")
    private SysMenu parent;

    @RelationOneToMany(selfField = "id", targetField = "parentId", orderBy = "order_no")
    private List<SysMenu> children;

    @JsonIgnore
    @RelationManyToMany(
            joinTable = "sys_role_menu",
            selfField = "id", joinSelfColumn = "menu_id",
            targetField = "id", joinTargetColumn = "role_id"
    )
    private List<SysRole> roles;
}
