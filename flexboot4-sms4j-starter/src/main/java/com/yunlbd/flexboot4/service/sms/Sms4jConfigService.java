package com.yunlbd.flexboot4.service.sms;

import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.service.sys.IExtendedService;

import java.util.List;

/**
 * 短信厂商配置 Service 接口
 */
public interface Sms4jConfigService extends IExtendedService<Sms4jConfig> {

    /**
     * 查询所有启用状态的厂商配置（status = 1）
     * 供 sms4j 动态数据源桥接层调用
     */
    List<Sms4jConfig> listEnabledConfigs();
}

