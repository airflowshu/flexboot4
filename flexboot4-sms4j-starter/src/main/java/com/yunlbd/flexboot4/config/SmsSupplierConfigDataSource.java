package com.yunlbd.flexboot4.config;

import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.core.datainterface.SmsReadConfig;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;
import org.dromara.sms4j.provider.factory.ProviderFactoryHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * sms4j 动态数据源桥接实现（第二种配置方式）
 *
 * <p>实现 {@link SmsReadConfig} 接口，将数据库中 {@code sms4j_config} 表的配置数据
 * 适配为 sms4j 框架所需的 {@link BaseConfig} 列表，从而驱动多厂商短信实例的动态加载。</p>
 *
 * <p>该 Bean 被 sms4j-spring-boot-starter 通过 {@code ObjectProvider<SmsReadConfig>}
 * 自动发现并注入 {@code SmsBlendsInitializer}，无需额外手动注册。</p>
 *
 * <p><b>刷新机制：</b>在通过 {@link Sms4jConfigService} 修改配置后，调用
 * {@link #reloadAll()} 或 {@link #reload(String)} 让 sms4j 重新加载对应的 SmsBlend 实例。</p>
 *
 * @author flexboot4
 * @since 2026-03-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsSupplierConfigDataSource implements SmsReadConfig {

    private final Sms4jConfigService sms4jConfigService;

    /**
     * 根据 configId 获取单个厂商配置
     *
     * @param configId sms4j 框架内部标识（对应 {@code sms4j_config.config_id}）
     * @return BaseConfig，若未找到则返回 null
     */
    @Override
    public BaseConfig getSupplierConfig(String configId) {
        return sms4jConfigService.listEnabledConfigs()
                .stream()
                .filter(c -> configId.equals(c.getConfigId()))
                .map(this::toBaseConfig)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有启用状态的厂商配置列表，供 sms4j 启动时批量构建 SmsBlend 实例
     */
    @Override
    public List<BaseConfig> getSupplierConfigList() {
        return sms4jConfigService.listEnabledConfigs()
                .stream()
                .map(this::toBaseConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 将数据库实体转换为 sms4j {@link BaseConfig} 实现类
     *
     * <p>通过 {@link ProviderFactoryHolder#requireForSupplier(String)} 查找对应厂商的
     * {@link BaseProviderFactory}，再通过 {@link BaseProviderFactory#getConfigClass()} 获取
     * 具体配置类并反射实例化，最后填充公共字段。厂商特有字段通过 extParams (JSON) 扩展支持。</p>
     *
     * @param entity 数据库配置记录
     * @return 对应厂商的 BaseConfig 子类实例，转换失败时返回 null
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private BaseConfig toBaseConfig(Sms4jConfig entity) {
        if (entity.getSupplierType() == null || entity.getSupplierType().isBlank()) {
            log.warn("[SMS] configId={} 缺少 supplierType，跳过", entity.getConfigId());
            return null;
        }
        try {
            // 通过 ProviderFactoryHolder 按 supplierType 查找对应厂商工厂
            BaseProviderFactory factory = ProviderFactoryHolder.requireForSupplier(entity.getSupplierType());
            if (factory == null) {
                log.warn("[SMS] 未找到 supplierType={} 对应的 ProviderFactory，跳过 configId={}",
                        entity.getSupplierType(), entity.getConfigId());
                return null;
            }
            // 通过工厂获取配置类并反射创建实例
            Class<? extends BaseConfig> configClass = (Class<? extends BaseConfig>) factory.getConfigClass();
            BaseConfig config = configClass.getDeclaredConstructor().newInstance();
            // 填充公共字段
            config.setConfigId(entity.getConfigId());
            config.setAccessKeyId(entity.getAccessKeyId());
            config.setAccessKeySecret(entity.getAccessKeySecret());
            config.setSignature(entity.getSignature());
            config.setTemplateId(entity.getTemplateId());
            config.setSdkAppId(entity.getSdkAppId());
            config.setWeight(entity.getWeight() != null ? entity.getWeight() : 1);
            return config;
        } catch (Exception e) {
            log.error("[SMS] 构建 BaseConfig 失败 configId={}, supplierType={}: {}",
                    entity.getConfigId(), entity.getSupplierType(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 重新加载指定 configId 的 SmsBlend 实例
     * <p>通常在更新单条配置后调用。</p>
     *
     * @param configId 需要刷新的配置 ID
     */
    public void reload(String configId) {
        log.info("[SMS] 重新加载 SmsBlend configId={}", configId);
        SmsFactory.reload(configId, this);
    }

    /**
     * 重新加载所有 SmsBlend 实例
     * <p>通常在批量修改配置或服务启动后主动刷新时调用。</p>
     */
    public void reloadAll() {
        log.info("[SMS] 重新加载所有 SmsBlend 实例");
        SmsFactory.reloadAll(this);
    }
}

