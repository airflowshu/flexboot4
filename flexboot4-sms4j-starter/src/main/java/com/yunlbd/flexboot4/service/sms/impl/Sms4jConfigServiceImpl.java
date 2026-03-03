package com.yunlbd.flexboot4.service.sms.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import com.yunlbd.flexboot4.mapper.Sms4jConfigMapper;
import com.yunlbd.flexboot4.service.sms.Sms4jConfigService;
import com.yunlbd.flexboot4.service.sys.impl.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 短信厂商配置 Service 实现
 */
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "sms4jConfig")
public class Sms4jConfigServiceImpl extends BaseServiceImpl<Sms4jConfigMapper, Sms4jConfig>
        implements Sms4jConfigService {

    @Override
    public List<Sms4jConfig> listEnabledConfigs() {
        return super.list(QueryWrapper.create()
                .where("status = 1"));
    }
}
