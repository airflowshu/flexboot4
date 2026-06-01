package com.yunlbd.flexboot4.controller.sms;

import com.yunlbd.flexboot4.common.ApiResult;
import com.yunlbd.flexboot4.common.annotation.OperLog;
import com.yunlbd.flexboot4.common.annotation.RequirePermission;
import com.yunlbd.flexboot4.common.enums.BusinessType;
import com.yunlbd.flexboot4.config.ApiTagGroup;
import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.controller.sys.EntityCrudController;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigCreateReq;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestReq;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestResult;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigUpdateReq;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import com.yunlbd.flexboot4.sms.Sms4jConfigTestStatus;
import com.yunlbd.flexboot4.vo.sms.Sms4jConfigDetailVO;
import com.yunlbd.flexboot4.vo.sms.Sms4jConfigListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
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
@Tag(name = "短信厂商配置", description = "Sms4jConfig - 短信厂商配置管理")
@ApiTagGroup(group = "短信管理")
public class Sms4jConfigController extends EntityCrudController<Sms4jConfigService, Sms4jConfig, String,
        Sms4jConfigCreateReq, Sms4jConfigUpdateReq, Sms4jConfigListVO, Sms4jConfigDetailVO> {

    public Sms4jConfigController(Sms4jConfigService service, SmsSupplierConfigDataSource smsSupplierConfigDataSource) {
        super(service, Sms4jConfig.class, Sms4jConfigListVO.class, Sms4jConfigDetailVO.class);
        this.smsSupplierConfigDataSource = smsSupplierConfigDataSource;
    }


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
    public ApiResult<Boolean> create(@RequestBody Sms4jConfigCreateReq request) {
        Sms4jConfig entity = crudMapper.toEntity(request);
        if (entity.getConfigId() == null || entity.getConfigId().isBlank()) {
            entity.setConfigId(UUID.randomUUID().toString().replace("-", ""));
        }
        entity.setTestStatus(Sms4jConfigTestStatus.UNTESTED);
        entity.setLastTestMessage("");
        entity.setLastTestTime(null);
        boolean ok = service.save(entity);
        if (ok) {
            smsSupplierConfigDataSource.reloadAll();
        }
        return ApiResult.success(ok);
    }

    /**
     * 更新厂商配置
     * <p>configId 字段不允许修改，始终保留原值。保存成功后触发全量热刷新。</p>
     */
    @Override
    @Operation(summary = "更新厂商配置", description = "更新短信厂商配置，configId 不可变更。")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public ApiResult<Boolean> update(@PathVariable String id, @RequestBody Sms4jConfigUpdateReq request) {
        Sms4jConfig entity = service.getById(id);
        if (entity == null) {
            throw new IllegalArgumentException("数据不存在: " + id);
        }
        SmsSendingConfigSnapshot before = SmsSendingConfigSnapshot.from(entity);
        crudMapper.updateEntity(request, entity);
        boolean shouldResetTestStatus = before.changed(SmsSendingConfigSnapshot.from(entity));
        // configId 不允许通过更新接口变更，强制置空让 updateById ignoreNulls 保留原值
        entity.setConfigId(null);
        boolean ok = service.updateById(entity, true);
        if (ok && shouldResetTestStatus) {
            service.resetTestStatus(id);
        }
        if (ok) {
            smsSupplierConfigDataSource.reloadAll();
        }
        return ApiResult.success(ok);
    }

    @Operation(summary = "测试厂商配置", description = "使用当前短信厂商配置真实发送一条测试短信，并记录测试状态。")
    @RequirePermission("sms4j:config:test")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/test")
    public ApiResult<Sms4jConfigTestResult> test(@PathVariable String id,
                                                  @RequestBody Sms4jConfigTestReq request) {
        return ApiResult.success(service.testConfig(id, request));
    }

    @Override
    @Operation(summary = "删除厂商配置", description = "删除短信厂商配置，保存后触发全量热刷新。")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public ApiResult<Boolean> remove(@PathVariable String id) {
        boolean ok = service.removeById(id);
        if (ok) {
            smsSupplierConfigDataSource.reloadAll();
        }
        return ApiResult.success(ok);
    }

    @Override
    @Operation(summary = "批量删除厂商配置", description = "批量删除短信厂商配置，保存后触发全量热刷新。")
    @OperLog(title = "短信厂商配置", businessType = BusinessType.DELETE)
    @DeleteMapping
    public ApiResult<Boolean> removeBatch(@RequestBody Collection<String> ids) {
        boolean ok = service.removeByIds(ids);
        if (ok) {
            smsSupplierConfigDataSource.reloadAll();
        }
        return ApiResult.success(ok);
    }

    private record SmsSendingConfigSnapshot(
            String supplierType,
            String accessKeyId,
            String accessKeySecret,
            String signature,
            String templateId,
            String sdkAppId,
            Map<String, Object> extParams
    ) {

        private static SmsSendingConfigSnapshot from(Sms4jConfig config) {
            return new SmsSendingConfigSnapshot(
                    trimToEmpty(config.getSupplierType()),
                    trimToEmpty(config.getAccessKeyId()),
                    trimToEmpty(config.getAccessKeySecret()),
                    trimToEmpty(config.getSignature()),
                    trimToEmpty(config.getTemplateId()),
                    trimToEmpty(config.getSdkAppId()),
                    config.getExtParams()
            );
        }

        private boolean changed(SmsSendingConfigSnapshot other) {
            return !Objects.equals(this, other);
        }

        private static String trimToEmpty(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
