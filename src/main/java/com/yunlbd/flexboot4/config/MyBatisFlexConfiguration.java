package com.yunlbd.flexboot4.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.listener.GlobalDictSetListener;
import com.yunlbd.flexboot4.listener.MybatisInsertListener;
import com.yunlbd.flexboot4.listener.MybatisUpdateListener;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisFlexConfiguration {

    public MyBatisFlexConfiguration() {

        MybatisInsertListener mybatisInsertListener = new MybatisInsertListener();
        MybatisUpdateListener mybatisUpdateListener = new MybatisUpdateListener();
        GlobalDictSetListener globalDictSetListener = new GlobalDictSetListener();
        FlexGlobalConfig config = FlexGlobalConfig.getDefaultConfig();

        //设置BaseEntity类启用
        config.registerInsertListener(mybatisInsertListener, BaseEntity.class);
        config.registerUpdateListener(mybatisUpdateListener, BaseEntity.class);
        config.registerSetListener(globalDictSetListener, BaseEntity.class);
    }
}
