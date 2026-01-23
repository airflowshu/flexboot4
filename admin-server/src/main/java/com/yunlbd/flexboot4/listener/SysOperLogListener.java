package com.yunlbd.flexboot4.listener;

import com.yunlbd.flexboot4.entity.SysOperLog;
import com.yunlbd.flexboot4.event.SysOperLogEvent;
import com.yunlbd.flexboot4.service.SysOperLogService;
import com.yunlbd.flexboot4.util.LogDesensitizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SysOperLogListener {

    private final SysOperLogService sysOperLogService;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOperLog(SysOperLogEvent event) {
        SysOperLog sysOperLog = event.getSysOperLog();
        try {
            // 数据脱敏处理
            sysOperLog.setOperParam(LogDesensitizationUtils.desensitize(sysOperLog.getOperParam()));
            sysOperLog.setJsonResult(LogDesensitizationUtils.desensitize(sysOperLog.getJsonResult()));
            
            sysOperLogService.save(sysOperLog);
        } catch (Exception e) {
            log.error("Async record oper log failed: {}", e.getMessage(), e);
        }
    }
}
