package com.yunlbd.flexboot4.controller.ops;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.controller.sys.BaseCrudController;
import com.yunlbd.flexboot4.controller.sys.CrudExcelSupport;
import com.yunlbd.flexboot4.controller.sys.CrudFieldPolicy;
import com.yunlbd.flexboot4.converter.ops.SysConfigCrudMapper;
import com.yunlbd.flexboot4.dto.ops.SysConfigCreateReq;
import com.yunlbd.flexboot4.dto.ops.SysConfigUpdateReq;
import com.yunlbd.flexboot4.entity.ops.SysConfig;
import com.yunlbd.flexboot4.excel.ops.SysConfigExportRow;
import com.yunlbd.flexboot4.excel.ops.SysConfigImportRow;
import com.yunlbd.flexboot4.service.ops.SysConfigService;
import com.yunlbd.flexboot4.vo.ops.SysConfigDetailVO;
import com.yunlbd.flexboot4.vo.ops.SysConfigListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "系统配置", description = "SysConfig - 系统参数配置管理")
@ApiTagGroup(group = "运维管理")
public class SysConfigController extends BaseCrudController<SysConfigService, SysConfig, String,
        SysConfigCreateReq, SysConfigUpdateReq, SysConfigListVO, SysConfigDetailVO> {

    private final SysConfigCrudMapper mapper;

    public SysConfigController(SysConfigService service, SysConfigCrudMapper mapper) {
        super(service, mapper);
        this.mapper = mapper;
    }


    @Override
    public Class<SysConfig> getEntityClass() {
        return SysConfig.class;
    }

    @Override
    protected CrudFieldPolicy fieldPolicy() {
        return CrudFieldPolicy.same(List.of(
                "id", "configKey", "configValue", "configType", "description",
                "status", "remark", "createTime", "lastModifyTime"
        ));
    }

    @Override
    protected CrudExcelSupport<SysConfig, ?, ?> excelSupport() {
        return CrudExcelSupport.of(SysConfigExportRow.class, SysConfigImportRow.class, mapper::toExportRow, null);
    }

    @Operation(summary = "根据键获取配置值", description = "根据配置键获取配置值")
    @RequirePermission("sys:config:list")
    @GetMapping("/value/{configKey}")
    public ApiResult<String> getConfigValue(@PathVariable String configKey) {
        String value = service.getConfigValue(configKey);
        return ApiResult.success(value);
    }

    @Operation(summary = "批量获取配置", description = "根据配置键列表批量获取配置")
    @RequirePermission("sys:config:list")
    @PostMapping("/values")
    public ApiResult<Map<String, String>> getConfigValues(@RequestBody List<String> configKeys) {
        Map<String, String> result = new java.util.HashMap<>();
        for (String key : configKeys) {
            result.put(key, service.getConfigValue(key));
        }
        return ApiResult.success(result);
    }

    @Operation(summary = "检查配置是否启用", description = "检查配置键是否已启用")
    @RequirePermission("sys:config:list")
    @GetMapping("/enabled/{configKey}")
    public ApiResult<Boolean> isEnabled(@PathVariable String configKey) {
        boolean enabled = service.isEnabled(configKey);
        return ApiResult.success(enabled);
    }

    @Operation(summary = "根据键和类型获取配置值", description = "根据配置键和类型获取配置值，支持STRING/NUMBER/BOOLEAN/JSON类型转换")
    @RequirePermission("sys:config:list")
    @GetMapping("/value/{configKey}/{configType}")
    public ApiResult<Object> getConfigValueAs(
            @PathVariable String configKey,
            @PathVariable String configType) {
        Object value = service.getConfigValueAs(configKey, configType);
        return ApiResult.success(value);
    }
}
