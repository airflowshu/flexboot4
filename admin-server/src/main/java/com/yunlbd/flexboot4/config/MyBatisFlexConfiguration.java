package com.yunlbd.flexboot4.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.table.TableManager;
import com.yunlbd.flexboot4.entity.BaseEntity;
import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.listener.GlobalDictSetListener;
import com.yunlbd.flexboot4.listener.MybatisInsertListener;
import com.yunlbd.flexboot4.listener.MybatisUpdateListener;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

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
        config.registerSetListener(globalDictSetListener, SysOperLog.class);
        
        // 配置动态表名处理器
        TableManager.setDynamicTableProcessor(tableName -> {
            // 如果表名已经包含 _YYYY_qQ 后缀，说明是完整表名，直接返回
            if (tableName.matches(".*_\\d{4}_q[1-4]$")) {
                return tableName;
            }
            // 如果是原始的 sys_oper_log，则追加当前季度后缀
            if ("sys_oper_log".equals(tableName)) {
                LocalDate now = LocalDate.now();
                int year = now.getYear();
                int month = now.getMonthValue();
                int quarter = (month - 1) / 3 + 1;
                return tableName + "_" + year + "_q" + quarter;
            }
            return tableName;
        });
    }
}
