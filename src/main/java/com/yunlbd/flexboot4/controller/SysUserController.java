package com.yunlbd.flexboot4.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.relation.RelationManager;
import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class SysUserController extends BaseController<SysUserService, SysUser, String> {

    @Override
    protected Class<SysUser> getEntityClass() {
        return SysUser.class;
    }

    @Operation(summary = "获取用户信息", description = "获取登录认证用户信息")
    @GetMapping("/info")
    public ApiResult<Map<String, Object>> getUserInfo() {
        SysUser user = SecurityUtils.getSysUser();
        if (user == null) {
            return ApiResult.error("User not logged in");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("avatar", user.getAvatar());
        info.put("roles", user.getRoles() != null 
                ? user.getRoles().stream().map(SysRole::getRoleValue).collect(Collectors.toList()) 
                : new ArrayList<>());
        
        return ApiResult.success(info);
    }

    @Override
    @Operation(summary = "分页查询", description = "分页查询用户列表，包含部门信息")
    @PostMapping("/page")
    public ApiResult<Page<SysUser>> page(@RequestBody SearchDto searchDto) {
        Page<SysUser> page = new Page<>(searchDto.getPageNumber(), searchDto.getPageSize());
        QueryWrapper queryWrapper = buildQueryWrapper(searchDto);
        service.page(page, queryWrapper);
        boolean hasRelationFilter = hasRelationFilter(searchDto);
        if (!hasRelationFilter) {
            RelationManager.queryRelations(service.getMapper(), page.getRecords());
        }
        return ApiResult.success(page);
    }

    private boolean hasRelationFilter(SearchDto dto) {
        if (dto == null) return false;
        if (dto.getItems() != null) {
            for (SearchDto.SearchItem it : dto.getItems()) {
                if (containsRelationPath(it)) return true;
            }
        }
        if (dto.getOrders() != null) {
            for (SearchDto.OrderItem od : dto.getOrders()) {
                if (od.getColumn() != null && od.getColumn().contains(".")) return true;
            }
        }
        return false;
    }

    private boolean containsRelationPath(SearchDto.SearchItem it) {
        if (it.getField() != null && it.getField().contains(".")) return true;
        if (it.getChildren() != null) {
            for (SearchDto.SearchItem c : it.getChildren()) {
                if (containsRelationPath(c)) return true;
            }
        }
        return false;
    }

}
