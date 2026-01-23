package com.yunlbd.flexboot4.controller.sys;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysUser;
import com.yunlbd.flexboot4.service.SysUserService;
import com.yunlbd.flexboot4.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "SysUser - 用户管理")
public class SysUserController extends BaseController<SysUserService, SysUser, String> {

    @Override
    public Class<SysUser> getEntityClass() {
        return SysUser.class;
    }

    private final PasswordEncoder passwordEncoder;

    @Override
    @Operation(summary = "Create", description = "Create entity.")
    @OperLog(title = "新建用户", businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResult<Boolean> create(@RequestBody SysUser user) {
        //新增用户，默认密码111111，
        // 重置密码需通过忘记密码，邮件方式重置;或者管理员才有操作权限reset
        user.setPassword(passwordEncoder.encode("111111"));
        return ApiResult.success(service.save(user));
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

 

}
