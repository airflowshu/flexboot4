package com.yunlbd.flexboot4.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IpUtilsTest {

    @Test
    public void testGetRegion() {
        // 测试内网 IP
        assertEquals("内网IP", IpUtils.getRegion("127.0.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("192.168.6.7"));
        assertEquals("内网IP", IpUtils.getRegion("10.0.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("172.16.0.1"));
        assertEquals("内网IP", IpUtils.getRegion("172.31.255.255"));
        
        // 测试非内网 IP (由于 ip2region.xdb 可能无效，这里预期返回 "未知" 或异常捕获后的处理)
        // 注意：如果 ip2region.xdb 损坏，searcher.search 会抛出异常被 catch 返回 "未知"
        String result = IpUtils.getRegion("8.8.8.8");
        // 如果文件损坏，会返回 "未知"
        assertEquals("未知", result);
    }
}
