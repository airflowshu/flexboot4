package com.yunlbd.flexboot4.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RouteMeta implements Serializable {
    private String title;
    private String icon;
    private String activeIcon;
    private Boolean hideMenu;
    private Integer order;
    private String badge;
    private String badgeType;
    private String badgeVariants;
    private String link;
    private String iframeSrc;
    private Boolean affixTab;
    private Boolean hideChildrenInMenu;
    private Boolean hideBreadcrumb;
    private Boolean hideTab;
    private Boolean keepAlive;
    private List<String> authority;
    private Boolean menuVisibleWithForbidden;
}
