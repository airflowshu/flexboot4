package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.ops.SysConfig;
import com.yunlbd.flexboot4.service.ops.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统参数配置表 Controller
 *
 * @author Wangts
 * @since 2026年01月29日
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "SysConfig - 系统参数配置管理")
public class SysConfigController extends BaseController<SysConfigService, SysConfig, String> {

    @Override
    public Class<SysConfig> getEntityClass() {
        return SysConfig.class;
    }

    @Operation(summary = "根据键获取配置值", description = "根据配置键获取配置值")
    @GetMapping("/value/{configKey}")
    public ApiResult<String> getConfigValue(@PathVariable String configKey) {
        String value = service.getConfigValue(configKey);
        return ApiResult.success(value);
    }

    @Operation(summary = "批量获取配置", description = "根据配置键列表批量获取配置")
    @PostMapping("/values")
    public ApiResult<Map<String, String>> getConfigValues(@RequestBody List<String> configKeys) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String key : configKeys) {
            result.put(key, service.getConfigValue(key));
        }
        return ApiResult.success(result);
    }

    @Operation(summary = "检查配置是否启用", description = "检查配置键是否已启用")
    @GetMapping("/enabled/{configKey}")
    public ApiResult<Boolean> isEnabled(@PathVariable String configKey) {
        boolean enabled = service.isEnabled(configKey);
        return ApiResult.success(enabled);
    }

    @Operation(summary = "根据键和类型获取配置值", description = "根据配置键和类型获取配置值，支持STRING/NUMBER/BOOLEAN/JSON类型转换")
    @GetMapping("/value/{configKey}/{configType}")
    public ApiResult<Object> getConfigValueAs(
            @PathVariable String configKey,
            @PathVariable String configType) {
        Object value = service.getConfigValueAs(configKey, configType);
        return ApiResult.success(value);
    }
}