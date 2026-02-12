package com.yunlbd.flexboot4.service.ops;

import com.yunlbd.flexboot4.entity.ops.SysConfig;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

/**
 * 系统参数配置表 Service接口
 *
 * @author Wangts
 * @since 2026年01月29日
 */
public interface SysConfigService extends IExtendedService<SysConfig> {

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值，如果不存在或已禁用则返回null
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，如果不存在则返回默认值
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 根据配置键判断是否启用
     *
     * @param configKey 配置键
     * @return true-启用, false-禁用或不存在
     */
    boolean isEnabled(String configKey);

    /**
     * 根据配置键获取配置值并转换为指定类型
     *
     * @param configKey 配置键
     * @param configType 配置类型: STRING/NUMBER/BOOLEAN/JSON
     * @param <T> 目标类型
     * @return 转换后的配置值，如果不存在返回null
     */
    <T> T getConfigValueAs(String configKey, String configType);

    /**
     * 根据配置键获取配置值并转换为指定类型
     *   使用示例
     * <p>
     *   // 数组类型
     *   List<String> tags = sysConfigService.getConfigValueAs("system.tags", "ARRAY");
     *   List<Integer> ids = sysConfigService.getConfigValueAs("feature.ids", "ARRAY", List.of());
     * <p>
     *   // JSON类型（自动识别）
     *   // config_value = {"name":"test","items":[1,2,3]}
     *   Map<String, Object> json = sysConfigService.getConfigValueAs("ai.config", "JSON");
     * <p>
     *     // 获取数值配置
     *   Integer maxSize = sysConfigService.getConfigValueAs("file.maxSize", "NUMBER", 10);
     * <p>
     *   // 获取布尔配置
     *   boolean enabled = sysConfigService.getConfigValueAs("feature.xx", "BOOLEAN", false);
     * <p>
     *   // 获取JSON配置
     *   Map<String, Object> config = sysConfigService.getConfigValueAs("ai.model", "JSON", Map.of());
         *
     *   使用示例
     * <p>
     *   // 数组类型
     *   List<String> tags = sysConfigService.getConfigValueAs("system.tags", "ARRAY");
     *   List<Integer> ids = sysConfigService.getConfigValueAs("feature.ids", "ARRAY", List.of());
     * <p>
     *   // JSON类型（自动识别）
     *   // config_value = {"name":"test","items":[1,2,3]}
     *   Map<String, Object> json = sysConfigService.getConfigValueAs("ai.config", "JSON");
     * <p>
     *     // 获取数值配置
     *   Integer maxSize = sysConfigService.getConfigValueAs("file.maxSize", "NUMBER", 10);
     * <p>
     *   // 获取布尔配置
     *   boolean enabled = sysConfigService.getConfigValueAs("feature.xx", "BOOLEAN", false);
     * <p>
     *   // 获取JSON配置
     *   Map<String, Object> config = sysConfigService.getConfigValueAs("ai.model", "JSON", Map.of());
     * @param configKey 配置键
     * @param configType 配置类型: STRING/NUMBER/BOOLEAN/JSON
     * @param defaultValue 默认值
     * @param <T> 目标类型
     * @return 转换后的配置值
     */
    <T> T getConfigValueAs(String configKey, String configType, T defaultValue);
}