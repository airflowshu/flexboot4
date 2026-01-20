package com.yunlbd.flexboot4.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class LogTableUtils {

    /**
     * 根据时间范围获取所有季度表名
     * @param start 开始时间 (包含)
     * @param end 结束时间 (包含)
     * @return 表名列表
     */
    public static List<String> getTableNames(LocalDateTime start, LocalDateTime end) {
        List<String> tables = new ArrayList<>();
        
        // 如果没有时间范围，默认返回当前季度的表名
        if (start == null || end == null) {
            tables.add(getCurrentQuarterTableName());
            return tables;
        }

        // 规范化时间：开始时间取季度初，结束时间取季度末（虽然逻辑上按月推进即可，但为了严谨）
        LocalDateTime current = start;
        
        // 循环直到当前时间超过结束时间
        // 注意：这里按月推进，计算每个月所属的季度表名，利用 Set 或 contains 去重
        while (!current.isAfter(end)) {
            String tableName = getQuarterTableName(current);
            if (!tables.contains(tableName)) {
                tables.add(tableName);
            }
            // 推进到下个月1号，避免日期的边界问题（比如1月31日加1个月可能变成2月28日）
            current = current.plusMonths(1).withDayOfMonth(1);
        }
        
        // 确保包含结束时间所在的季度（防止上面的循环因为步长问题漏掉最后一个月，虽按月推进一般不会）
        String endTable = getQuarterTableName(end);
        if (!tables.contains(endTable)) {
            tables.add(endTable);
        }
        
        return tables;
    }

    /**
     * 获取指定时间对应的季度表名
     */
    public static String getQuarterTableName(LocalDateTime date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        int quarter = (month - 1) / 3 + 1;
        return "sys_oper_log_" + year + "_q" + quarter;
    }

    /**
     * 获取当前季度的表名
     */
    public static String getCurrentQuarterTableName() {
        return getQuarterTableName(LocalDateTime.now());
    }
    
    /**
     * 解析日期字符串，支持 yyyy-MM-dd 和 yyyy-MM-dd HH:mm:ss
     */
    public static LocalDateTime parseDate(Object val) {
        if (val == null) return null;
        String str = val.toString();
        try {
            if (str.length() == 10) {
                return LocalDate.parse(str).atStartOfDay();
            } else if (str.length() >= 19) {
                // 截取前19位，适配可能带毫秒的情况
                return LocalDateTime.parse(str.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }
}
