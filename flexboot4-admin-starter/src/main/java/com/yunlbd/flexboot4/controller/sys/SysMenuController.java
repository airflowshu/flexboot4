package com.yunlbd.flexboot4.controller.sys;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.converter.sys.SysMenuCrudMapper;
import com.yunlbd.flexboot4.dto.sys.SysMenuCreateReq;
import com.yunlbd.flexboot4.dto.sys.SysMenuUpdateReq;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.entity.sys.SysMenu;
import com.yunlbd.flexboot4.excel.sys.SysMenuExportRow;
import com.yunlbd.flexboot4.excel.sys.SysMenuImportRow;
import com.yunlbd.flexboot4.service.sys.SysMenuService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import com.yunlbd.flexboot4.vo.sys.SysMenuDetailVO;
import com.yunlbd.flexboot4.vo.sys.SysMenuListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menu")
@Tag(name = "菜单管理", description = "SysMenu - 菜单管理")
@ApiTagGroup(group = "系统管理")
public class SysMenuController extends BaseCrudController<SysMenuService, SysMenu, String,
        SysMenuCreateReq, SysMenuUpdateReq, SysMenuListVO, SysMenuDetailVO> {

    private final SysMenuCrudMapper mapper;

    public SysMenuController(SysMenuService service, SysMenuCrudMapper mapper) {
        super(service, mapper);
        this.mapper = mapper;
    }


    @Override
    public Class<SysMenu> getEntityClass() {
        return SysMenu.class;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "parentId", "path", "name", "component", "redirect", "title",
                "icon", "orderNo", "hideInMenu", "keepAlive", "activeIcon", "badge",
                "badgeType", "badgeVariants", "link", "iframeSrc", "affixTab",
                "hideChildrenInMenu", "hideInBreadcrumb", "hideInTab",
                "menuVisibleWithForbidden", "authority", "authCode", "type", "status",
                "remark", "createTime", "lastModifyTime"
        ));
    }

    @Override
    protected CrudExcelSupport<SysMenu, ?, ?> excelSupport() {
        return CrudExcelSupport.of(SysMenuExportRow.class, SysMenuImportRow.class, mapper::toExportRow, null);
    }

    @Operation(summary = "获取菜单权限", description = "获取登录认证用户所拥有的所有菜单访问数据")
    @RequirePermission(skip = true)
    @GetMapping("/all")
    public ApiResult<List<VueRoute>> getAllMenus() {
        String userId = SecurityUtils.getUserId();
        if (userId == null) {
            userId = "0"; // Fallback or handle unauthenticated case
        }
        return ApiResult.success(service.getUserMenus(userId));
    }

    @Operation(summary = "菜单名称是否存在", description = "根据名称检测是否存在其他菜单，更新时可排除自身ID")
    @RequirePermission("sys:menu:list")
    @GetMapping("/name-exists")
    public ApiResult<Boolean> isMenuNameExists(@RequestParam("name") String name,
                                               @RequestParam(value = "id", required = false) String id) {
        QueryWrapper query = QueryWrapper.create()
                .where(SysMenu::getName).eq(name);
        if (id != null && !id.isBlank()) {
            query.and(SysMenu::getId).ne(id);
        }
        return ApiResult.success(service.count(query) > 0);
    }

    @Operation(summary = "菜单路径是否存在", description = "根据路径检测是否存在其他菜单，更新时可排除自身ID")
    @RequirePermission("sys:menu:list")
    @GetMapping("/path-exists")
    public ApiResult<Boolean> isMenuPathExists(@RequestParam("path") String path,
                                               @RequestParam(value = "id", required = false) String id) {
        QueryWrapper query = QueryWrapper.create()
                .where(SysMenu::getPath).eq(path);
        if (id != null && !id.isBlank()) {
            query.and(SysMenu::getId).ne(id);
        }
        return ApiResult.success(service.count(query) > 0);
    }

}
