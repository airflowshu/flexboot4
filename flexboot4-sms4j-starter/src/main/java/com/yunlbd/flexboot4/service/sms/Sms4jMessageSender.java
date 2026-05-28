package com.yunlbd.flexboot4.service.sms;

import com.yunlbd.flexboot4.config.SmsSupplierConfigDataSource;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.sms.SmsMessageRequest;
import com.yunlbd.flexboot4.sms.SmsMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Sms4jMessageSender implements SmsMessageSender {

    private final Sms4jConfigService sms4jConfigService;
    private final SmsSupplierConfigDataSource smsSupplierConfigDataSource;

    @Override
    public void send(SmsMessageRequest request) {
        Sms4jConfig config = resolveConfig(request.configId());
        if (config == null) {
            throw new IllegalStateException("未找到可用短信配置");
        }

        String templateId = firstNotBlank(request.templateId(), config.getTemplateId());
        if (templateId == null) {
            throw new IllegalStateException("短信模板ID未配置");
        }

        var smsBlend = SmsFactory.getSmsBlend(config.getConfigId());
        if (smsBlend == null) {
            log.info("[SMS] SmsBlend not initialized, reloading configId={}", config.getConfigId());
            smsSupplierConfigDataSource.reload(config.getConfigId());
            smsBlend = SmsFactory.getSmsBlend(config.getConfigId());
        }
        if (smsBlend == null) {
            throw new IllegalStateException("短信通道未初始化: " + config.getConfigId());
        }

        LinkedHashMap<String, String> params = new LinkedHashMap<>(request.templateParams());
        SmsResponse response = smsBlend.sendMessage(request.phone(), templateId, params);
        log.info("[SMS] message send result supplier={}, configId={}, success={}, detail={}",
                config.getSupplierType(), config.getConfigId(),
                response != null && response.isSuccess(),
                responseSummary(response));
        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("短信发送失败: " + responseSummary(response));
        }
    }

    private Sms4jConfig resolveConfig(String configId) {
        List<Sms4jConfig> configs = sms4jConfigService.listEnabledConfigs();
        if (configs == null || configs.isEmpty()) {
            return null;
        }
        if (configId != null && !configId.isBlank()) {
            return configs.stream()
                    .filter(config -> configId.equals(config.getConfigId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("指定短信配置不存在或未启用: " + configId));
        }
        return configs.stream()
                .filter(config -> Integer.valueOf(1).equals(config.getIsDefault()))
                .findFirst()
                .orElse(configs.getFirst());
    }

    private static String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static String responseSummary(SmsResponse response) {
        if (response == null) {
            return "厂商未返回发送结果";
        }
        Object data = response.getData();
        if (data == null) {
            return response.isSuccess() ? "发送成功" : "厂商未返回错误详情";
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
