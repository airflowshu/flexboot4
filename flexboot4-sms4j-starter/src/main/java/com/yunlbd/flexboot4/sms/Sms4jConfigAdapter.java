package com.yunlbd.flexboot4.sms;

import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.springframework.beans.BeanWrapperImpl;

@Slf4j
public final class Sms4jConfigAdapter {

    private Sms4jConfigAdapter() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static BaseConfig toBaseConfig(Sms4jConfig entity) {
        if (entity.getSupplierType() == null || entity.getSupplierType().isBlank()) {
            log.warn("[SMS] configId={} 缺少 supplierType，跳过", entity.getConfigId());
            return null;
        }
        try {
            BaseProviderFactory factory = ProviderFactoryHolder.requireForSupplier(entity.getSupplierType());
            if (factory == null) {
                log.warn("[SMS] 未找到 supplierType={} 对应的 ProviderFactory，跳过 configId={}",
                        entity.getSupplierType(), entity.getConfigId());
                return null;
            }
            Class<? extends BaseConfig> configClass = (Class<? extends BaseConfig>) factory.getConfigClass();
            BaseConfig config = configClass.getDeclaredConstructor().newInstance();
            config.setConfigId(entity.getConfigId());
            config.setAccessKeyId(entity.getAccessKeyId());
            config.setAccessKeySecret(entity.getAccessKeySecret());
            config.setSignature(entity.getSignature());
            config.setTemplateId(entity.getTemplateId());
            config.setSdkAppId(entity.getSdkAppId());
            config.setWeight(entity.getWeight() != null ? entity.getWeight() : 1);
            applyExtParams(entity, config);
            return config;
        } catch (Exception e) {
            log.error("[SMS] 构建 BaseConfig 失败 configId={}, supplierType={}: {}",
                    entity.getConfigId(), entity.getSupplierType(), e.getMessage(), e);
            return null;
        }
    }

    private static void applyExtParams(Sms4jConfig entity, BaseConfig config) {
        if (entity.getExtParams() == null || entity.getExtParams().isEmpty()) {
            return;
        }
        BeanWrapperImpl wrapper = new BeanWrapperImpl(config);
        entity.getExtParams().forEach((key, value) -> {
            if (key == null || key.isBlank() || !wrapper.isWritableProperty(key)) {
                return;
            }
            try {
                wrapper.setPropertyValue(key, value);
            } catch (Exception e) {
                log.warn("[SMS] 忽略无法写入的扩展配置 configId={}, field={}: {}",
                        entity.getConfigId(), key, e.getMessage());
            }
        });
    }
}
