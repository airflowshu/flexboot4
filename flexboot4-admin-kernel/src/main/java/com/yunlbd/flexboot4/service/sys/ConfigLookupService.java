package com.yunlbd.flexboot4.service.sys;

public interface ConfigLookupService {

    String getConfigValue(String configKey);

    String getConfigValue(String configKey, String defaultValue);

    boolean isEnabled(String configKey);

    <T> T getConfigValueAs(String configKey, String configType);

    <T> T getConfigValueAs(String configKey, String configType, T defaultValue);
}
