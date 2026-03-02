package com.yunlbd.flexboot4.entity.sys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
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
@Table("sys_menu")
@Schema(name = "SysMenu")
public class SysMenu extends BaseEntity {

    @Schema(title = "父菜单ID")
    private String parentId;

    @Schema(title = "路由路径")
    private String path;

    @Schema(title = "路由名称")
    private String name;

    @Schema(title = "组件路径")
    private String component;

    @Schema(title = "重定向地址")
    private String redirect;

    @Schema(title = "菜单标题")
    private String title;

    @Schema(title = "菜单图标")
    private String icon;

    @Schema(title = "排序号")
    private Integer orderNo;

    @Schema(title = "是否隐藏菜单")
    private Boolean hideMenu;

    @Schema(title = "是否缓存")
    private Boolean keepAlive;

    @Schema(title = "活跃状态下的图标")
    private String activeIcon;

    @Schema(title = "徽章")
    private String badge;

    @Schema(title = "徽章类型（dot:圆点, normal:普通）")
    private String badgeType;

    @Schema(title = "徽章变种（primary:主色, error:错误色, success:成功色, warning:警告色）")
    private String badgeVariants;

    @Schema(title = "外链地址")
    private String link;

    @Schema(title = "iframe链接")
    private String iframeSrc;

    @Schema(title = "是否固定标签页")
    private Boolean affixTab;

    @Schema(title = "是否在菜单中隐藏子菜单")
    private Boolean hideChildrenInMenu;

    @Schema(title = "是否隐藏面包屑")
    private Boolean hideBreadcrumb;

    @Schema(title = "是否隐藏标签页")
    private Boolean hideTab;

    @Schema(title = "403时是否显示菜单")
    private Boolean menuVisibleWithForbidden;

    @Schema(title = "权限角色列表（逗号分隔）")
    private String authority;

    @Schema(title = "权限编码（如：sys:user:add）")
    private String authCode;

    @Schema(title = "菜单类型（catalog:目录, menu:菜单, button:按钮, embedded:嵌入, link:链接）")
    private String type;

    @Schema(title = "状态（0:禁用, 1:启用）")
    private Integer status;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "父菜单")
    @RelationManyToOne(selfField = "parentId", targetField = "id")
    private SysMenu parent;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, title = "子菜单列表")
    @RelationOneToMany(selfField = "id", targetField = "parentId", orderBy = "order_no")
    private List<SysMenu> children;

    @JsonIgnore
    @Schema(hidden = true)
    @RelationManyToMany(
            joinTable = "sys_role_menu",
            selfField = "id", joinSelfColumn = "menu_id",
            targetField = "id", joinTargetColumn = "role_id"
    )
    private List<SysRole> roles;
}
