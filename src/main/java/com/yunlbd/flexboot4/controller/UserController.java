package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.entity.SysRole;
import com.yunlbd.flexboot4.entity.SysUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

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
