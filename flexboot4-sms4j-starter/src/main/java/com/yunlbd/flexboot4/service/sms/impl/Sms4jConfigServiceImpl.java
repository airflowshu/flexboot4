package com.yunlbd.flexboot4.service.sms.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestReq;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestResult;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.mapper.Sms4jConfigMapper;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import com.yunlbd.flexboot4.sms.Sms4jConfigAdapter;
import com.yunlbd.flexboot4.sms.Sms4jConfigTestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yunlbd.flexboot4.entity.sms.table.Sms4jConfigTableDef.SMS4J_CONFIG;

/**
 * 短信厂商配置 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sms4jConfig")
public class Sms4jConfigServiceImpl extends BaseServiceImpl<Sms4jConfigMapper, Sms4jConfig>
        implements Sms4jConfigService {

    @Override
    public List<Sms4jConfig> listEnabledConfigs() {
        return cacheProxy().list(QueryWrapper.create()
                .where("status = 1"));
    }

    @Override
    public Sms4jConfigTestResult testConfig(String id, Sms4jConfigTestReq request) {
        Sms4jConfig config = getById(id);
        if (config == null) {
            throw new IllegalArgumentException("短信配置不存在: " + id);
        }
        if (request == null) {
            throw new IllegalArgumentException("测试参数不能为空");
        }

        String phone = trimToNull(request.phone());
        if (phone == null) {
            throw new IllegalArgumentException("测试手机号不能为空");
        }

        String templateId = firstNotBlank(request.templateId(), config.getTemplateId());
        if (templateId == null) {
            throw new IllegalArgumentException("短信模板ID未配置");
        }

        LocalDateTime testedAt = LocalDateTime.now();
        try {
            SmsBlend smsBlend = createTestSmsBlend(config);
            SmsResponse response = smsBlend.sendMessage(
                    phone,
                    templateId,
                    new LinkedHashMap<>(request.templateParams())
            );
            String message = responseSummary(response);
            boolean success = response != null && response.isSuccess();
            String status = success ? Sms4jConfigTestStatus.PASSED : Sms4jConfigTestStatus.FAILED;
            recordTestResult(id, status, testedAt, message);
            if (!success) {
                log.warn("[SMS] config test failed supplier={}, configId={}, detail={}",
                        config.getSupplierType(), config.getConfigId(), message);
            }
            return new Sms4jConfigTestResult(success, message, testedAt, status);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "短信测试发送失败" : e.getMessage();
            recordTestResult(id, Sms4jConfigTestStatus.FAILED, testedAt, message);
            log.warn("[SMS] config test failed supplier={}, configId={}, reason={}",
                    config.getSupplierType(), config.getConfigId(), message, e);
            return new Sms4jConfigTestResult(false, message, testedAt, Sms4jConfigTestStatus.FAILED);
        }
    }

    protected SmsBlend createTestSmsBlend(Sms4jConfig config) {
        BaseConfig baseConfig = Sms4jConfigAdapter.toBaseConfig(config);
        if (baseConfig == null) {
            throw new IllegalStateException("短信通道配置无效");
        }
        var factory = ProviderFactoryHolder.requireForSupplier(config.getSupplierType());
        if (factory == null) {
            throw new IllegalStateException("未找到短信厂商工厂: " + config.getSupplierType());
        }
        @SuppressWarnings({"rawtypes", "unchecked"})
        SmsBlend smsBlend = ((org.dromara.sms4j.provider.factory.BaseProviderFactory) factory).createSms(baseConfig);
        if (smsBlend == null) {
            throw new IllegalStateException("短信通道初始化失败: " + config.getConfigId());
        }
        return smsBlend;
    }

    protected void recordTestResult(String id, String status, LocalDateTime testedAt, String message) {
        String shortMessage = abbreviate(message, 500);
        UpdateChain.of(getMapper())
                .set(SMS4J_CONFIG.TEST_STATUS, status, true)
                .set(SMS4J_CONFIG.LAST_TEST_TIME, testedAt, true)
                .set(SMS4J_CONFIG.LAST_TEST_MESSAGE, shortMessage, true)
                .where(SMS4J_CONFIG.ID.eq(id))
                .update();
        bumpTableVersion(Sms4jConfig.class);
    }

    @Override
    public boolean resetTestStatus(String id) {
        boolean ok = UpdateChain.of(getMapper())
                .set(SMS4J_CONFIG.TEST_STATUS, Sms4jConfigTestStatus.UNTESTED, true)
                .set(SMS4J_CONFIG.LAST_TEST_TIME, null, true)
                .set(SMS4J_CONFIG.LAST_TEST_MESSAGE, "", true)
                .where(SMS4J_CONFIG.ID.eq(id))
                .update();
        if (ok) {
            bumpTableVersion(Sms4jConfig.class);
        }
        return ok;
    }

    private static String firstNotBlank(String first, String second) {
        String value = trimToNull(first);
        if (value != null) {
            return value;
        }
        return trimToNull(second);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String responseSummary(SmsResponse response) {
        if (response == null) {
            return "厂商未返回发送结果";
        }
        Object data = response.getData();
        if (data == null) {
            return response.isSuccess() ? "测试短信发送成功" : "厂商未返回错误详情";
        }

        String statusCode = valueOf(data, "statusCode");
        String statusMsg = valueOf(data, "statusMsg");
        if (statusCode != null || statusMsg != null) {
            StringBuilder summary = new StringBuilder();
            if (statusCode != null) {
                summary.append("statusCode=").append(statusCode);
            }
            if (statusMsg != null) {
                if (!summary.isEmpty()) {
                    summary.append(", ");
                }
                summary.append("statusMsg=").append(statusMsg);
            }
            return summary.toString();
        }
        return data.toString();
    }

    private static String valueOf(Object data, String key) {
        if (data instanceof Map<?, ?> map) {
            Object value = map.get(key);
            return value == null ? null : value.toString();
        }
        try {
            Method method = data.getClass().getMethod("getStr", String.class);
            Object value = method.invoke(data, key);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
