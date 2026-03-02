package com.yunlbd.flexboot4.listener;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SysOperLogListenerTest {

    @Test
    void recordOperLog_shouldBeTransactionalRequiresNew() throws Exception {
        Method method = SysOperLogListener.class.getMethod("recordOperLog", com.yunlbd.flexboot4.event.SysOperLogEvent.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}

