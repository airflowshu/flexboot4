package com.yunlbd.flexboot4.service.ops.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.ops.SysConfig;
import com.yunlbd.flexboot4.mapper.SysConfigMapper;
import com.yunlbd.flexboot4.service.ops.SysConfigService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 系统参数配置表 Service实现
 *
 * @author Wangts
 * @since 2026年01月29日
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sysConfig")
public class SysConfigServiceImpl extends BaseServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private static final Integer STATUS_ENABLED = 1;
    private static final String TYPE_STRING = "STRING";
    private static final String TYPE_NUMBER = "NUMBER";
    private static final String TYPE_BOOLEAN = "BOOLEAN";
    private static final String TYPE_JSON = "JSON";
    private static final String TYPE_ARRAY = "ARRAY";

    private final ObjectMapper objectMapper;

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        if (configKey == null || configKey.isBlank()) {
            return defaultValue;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getStatus, STATUS_ENABLED);

        SysConfig config = super.getOne(queryWrapper);

        if (config == null) {
            log.debug("配置键不存在或已禁用: {}", configKey);
            return defaultValue;
        }

        return config.getConfigValue();
    }

    @Override
    public boolean isEnabled(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            return false;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getStatus, 1);

        return super.count(queryWrapper) > 0;
    }

    @Override
    public <T> T getConfigValueAs(String configKey, String configType) {
        return getConfigValueAs(configKey, configType, null);
    }

    @Override
    public <T> T getConfigValueAs(String configKey, String configType, T defaultValue) {
        if (configKey == null || configKey.isBlank()) {
            return defaultValue;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getStatus, STATUS_ENABLED);

        SysConfig config = super.getOne(queryWrapper);

        if (config == null) {
            log.debug("配置键不存在或已禁用: {}", configKey);
            return defaultValue;
        }

        return convertValue(config.getConfigValue(), configType, defaultValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertValue(String value, String configType, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        try {
            return switch (configType.toUpperCase()) {
                case TYPE_NUMBER -> convertNumber(value, defaultValue);
                case TYPE_BOOLEAN -> convertBoolean(value, defaultValue);
                case TYPE_ARRAY -> convertArray(value, defaultValue);
                case TYPE_JSON -> convertJson(value, defaultValue);
                default -> (T) value;
            };
        } catch (Exception e) {
            log.warn("配置值转换失败: value={}, type={}, error={}", value, configType, e.getMessage());
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertNumber(String value, T defaultValue) {
        try {
            // 尝试解析为Long
            if (value.matches("^-?\\d+$")) {
                return (T) Long.valueOf(value);
            }
            // 尝试解析为Double
            return (T) Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertBoolean(String value, T defaultValue) {
        String lower = value.toLowerCase().trim();
        if ("true".equals(lower) || "1".equals(lower) || "yes".equals(lower)) {
            return (T) Boolean.TRUE;
        } else if ("false".equals(lower) || "0".equals(lower) || "no".equals(lower)) {
            return (T) Boolean.FALSE;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private <T> T convertArray(String value, T defaultValue) {
        try {
            return (T) objectMapper.readValue(value, new TypeReference<List<Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("数组解析失败: {}", value);
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertJson(String value, T defaultValue) {
        try {
            String trimmed = value.trim();
            if (trimmed.startsWith("{")) {
                // 对象
                return (T) objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            } else if (trimmed.startsWith("[")) {
                // 数组
                return (T) objectMapper.readValue(value, new TypeReference<List<Object>>() {});
            } else {
                // 简单值
                return (T) objectMapper.readValue(value, Object.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败: {}", value);
            return defaultValue;
        }
    }
}