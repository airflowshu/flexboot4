package com.yunlbd.flexboot4.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IpUtilsTest {

    @Test
    public void testGetRegion() {
        // 测试内网 IP
        assertEquals("内网IP", IpUtils.getRegion("127.0.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("192.168.6.7"));
        assertEquals("内网IP", IpUtils.getRegion("10.0.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("172.16.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("172.31.255.255"));
        
        String result = IpUtils.getRegion("8.8.8.8");
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertNotEquals("内网IP", result);
    }
}
