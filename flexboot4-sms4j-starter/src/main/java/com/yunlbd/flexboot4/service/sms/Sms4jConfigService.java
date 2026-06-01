package com.yunlbd.flexboot4.service.sms;

import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestReq;
import com.yunlbd.flexboot4.dto.sms.Sms4jConfigTestResult;
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

    /**
     * 使用指定配置真实发送一条测试短信，并记录测试状态。
     */
    Sms4jConfigTestResult testConfig(String id, Sms4jConfigTestReq request);

    /**
     * 将配置测试状态重置为未测试。
     */
    boolean resetTestStatus(String id);
}
