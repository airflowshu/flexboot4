package com.yunlbd.flexboot4.controller;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.util.SecurityUtils;
import com.yunlbd.flexboot4.dto.VueRoute;
import com.yunlbd.flexboot4.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/all")
    public ApiResult<List<VueRoute>> getAllMenus() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            userId = 0L; // Fallback or handle unauthenticated case
        }
        return ApiResult.success(sysMenuService.getUserMenus(userId));
    }
}
