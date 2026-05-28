package com.yunlbd.flexboot4.vo.sys;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenuListVO extends BaseAuditVO {
    private String parentId;
    private String path;
    private String name;
    private String component;
    private String redirect;
    private String title;
    private String icon;
    private Integer orderNo;
    private Boolean hideInMenu;
    private Boolean keepAlive;
    private String activeIcon;
    private String badge;
    private String badgeType;
    private String badgeVariants;
    private String link;
    private String iframeSrc;
    private Boolean affixTab;
    private Boolean hideChildrenInMenu;
    private Boolean hideInBreadcrumb;
    private Boolean hideInTab;
    private Boolean menuVisibleWithForbidden;
    private String authority;
    private String authCode;
    private String type;
    private Integer status;
    private List<SysMenuListVO> children;
}
