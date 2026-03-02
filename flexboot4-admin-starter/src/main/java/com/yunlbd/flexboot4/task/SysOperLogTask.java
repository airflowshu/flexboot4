package com.yunlbd.flexboot4.task;

import com.yunlbd.flexboot4.util.LogTableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 操作日志定时任务
 * 自动创建季度分表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysOperLogTask {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 每季度末（3,6,9,12月）的25号凌晨1点执行
     * 检查并创建下一个季度的日志表
     */
    @Scheduled(cron = "0 0 1 25 3,6,9,12 ?")
    public void createNextQuarterTable() {
        log.info("Start checking next quarter log table...");
        
        // 获取下个季度的第一天
        LocalDate nextQuarterDate = LocalDate.now().plusMonths(1);
        String tableName = LogTableUtils.getQuarterTableName(nextQuarterDate.atStartOfDay());
        
        createTableIfNotExists(tableName);
    }

    /**
     * 系统启动时也尝试检查当前季度和下个季度的表是否存在
     */
    @Scheduled(initialDelay = 10000, fixedDelay = Long.MAX_VALUE)
    public void initCheck() {
        // 当前季度
        createTableIfNotExists(LogTableUtils.getCurrentQuarterTableName());
        
        // 下个季度（防止临近换季时上线）
        LocalDate nextQuarterDate = LocalDate.now().plusMonths(1);
        createTableIfNotExists(LogTableUtils.getQuarterTableName(nextQuarterDate.atStartOfDay()));
    }

    private void createTableIfNotExists(String tableName) {
        try {
            // PostgreSQL 语法: CREATE TABLE IF NOT EXISTS target (LIKE source INCLUDING ALL)
            // 复制表结构、索引、约束等
            String sql = String.format("CREATE TABLE IF NOT EXISTS %s (LIKE sys_oper_log INCLUDING ALL)", tableName);
            jdbcTemplate.execute(sql);
            log.info("Check/Create log table success: {}", tableName);
        } catch (Exception e) {
            log.error("Failed to create log table: {}", tableName, e);
        }
    }
}
