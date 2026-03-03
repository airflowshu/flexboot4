package com.yunlbd.flexboot4.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yunlbd.flexboot4.entity.sms.Sms4jConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短信厂商配置 Mapper
 * <p>包路径与其他模块保持一致，统一由启动类 {@code @MapperScan("com.yunlbd.flexboot4.mapper")} 扫描。</p>
 */
@Mapper
public interface Sms4jConfigMapper extends BaseMapper<Sms4jConfig> {
}

