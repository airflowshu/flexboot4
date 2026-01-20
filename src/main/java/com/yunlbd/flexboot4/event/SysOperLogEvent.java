package com.yunlbd.flexboot4.event;

import com.yunlbd.flexboot4.entity.SysOperLog;
import org.springframework.context.ApplicationEvent;

public class SysOperLogEvent extends ApplicationEvent {
    public SysOperLogEvent(SysOperLog source) {
        super(source);
    }

    public SysOperLog getSysOperLog() {
        return (SysOperLog) getSource();
    }
}
