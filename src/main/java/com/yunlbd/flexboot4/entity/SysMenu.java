package com.yunlbd.flexboot4.entity;

import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    private Long parentId;
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
    private Integer type; // 0: dir, 1: menu, 2: button
    private Integer status;
}
