package com.yunlbd.flexboot4.controller.sms;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.controller.sys.BaseController;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 短信厂商配置 Controller
 * <p>
 * 提供短信厂商配置的 CRUD 管理接口。
 * 新增/更新/删除后自动触发 sms4j 配置热刷新，无需重启应用。
 * </p>
 */
@RestController
@RequestMapping("/api/admin/sms/config")
@RequiredArgsConstructor
@Tag(name = "短信厂商配置", description = "Sms4jConfig - 短信厂商配置管理")
@ApiTagGroup(group = "短信管理")
public class Sms4jConfigController extends BaseController<Sms4jConfigService, Sms4jConfig, String> {

    private final SmsSupplierConfigDataSource smsSupplierConfigDataSource;

    @Override
    public Class<Sms4jConfig> getEntityClass() {
        return Sms4jConfig.class;
    }

    /**
     * 新增厂商配置
     * <p>若未传入 configId，则自动生成一个全局唯一标识，保存成功后触发全量热刷新。</p>
     */
    @Override
    @Operation(summary = "新增厂商配置", description = "新增短信厂商配置，configId 未填时自动生成。")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.INSERT)
    @PostMapping
    public ApiResult<Boolean> create(@RequestBody Sms4jConfig entity) {
        if (entity.getConfigId() == null || entity.getConfigId().isBlank()) {
            entity.setConfigId(UUID.randomUUID().toString().replace("-", ""));
        }
        boolean ok = service.save(entity);
        if (ok) {
            smsSupplierConfigDataSource.reloadAll();
        }
        return ApiResult.success(ok);
    }

    /**
     * 更新厂商配置
     * <p>configId 字段不允许修改，始终保留原值。保存成功后按 configId 热刷新对应实例。</p>
     */
    @Override
    @Operation(summary = "更新厂商配置", description = "更新短信厂商配置，configId 不可变更。")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable String id, @RequestBody Sms4jConfig entity) {
        entity.setId(id);
        // configId 不允许通过更新接口变更，强制置空让 updateById ignoreNulls 保留原值
        entity.setConfigId(null);
        boolean ok = service.updateById(entity, true);
        if (ok) {
            // 重新查出完整记录获取 configId 后做精准刷新
            Sms4jConfig saved = service.getById(id);
            if (saved != null && saved.getConfigId() != null) {
                smsSupplierConfigDataSource.reload(saved.getConfigId());
            }
        }
        return ApiResult.success(ok);
    }

    // /**
    //  * 删除厂商配置后触发全量热刷新
    //  */
    // @Override
    // @Operation(summary = "删除厂商配置")
    // @OperLog(title = "短信厂商配置", businessType = BusinessType.DELETE)
    // @DeleteMapping("/{id}")
    // public ApiResult<Boolean> remove(@PathVariable String id) {
    //     boolean ok = service.removeById(id);
    //     if (ok) {
    //         smsSupplierConfigDataSource.reloadAll();
    //     }
    //     return ApiResult.success(ok);
    // }

}
