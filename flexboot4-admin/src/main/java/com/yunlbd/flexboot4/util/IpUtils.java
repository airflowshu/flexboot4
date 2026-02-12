package com.yunlbd.flexboot4.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;

@Slf4j
public class IpUtils {

    private static Searcher searcher;

    static {
        try {
            // 从 ClassPath 读取 ip2region.xdb 文件到内存
            ClassPathResource resource = new ClassPathResource("ip2region.xdb");
            InputStream inputStream = resource.getInputStream();
            byte[] cBuff = StreamUtils.copyToByteArray(inputStream);
            searcher = Searcher.newWithBuffer(cBuff);
            log.info("Ip2region loaded successfully.");
        } catch (Exception e) {
            log.error("Failed to load ip2region.xdb: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据 IP 获取物理地址
     *
     * @param ip IP地址
     * @return 地址信息 (国家|区域|省份|城市|ISP)
     */
    public static String getRegion(String ip) {
        if (searcher == null || StringUtils.isBlank(ip) || isInternalIp(ip)) {
            return "内网IP";
        }
        try {
            String region = searcher.search(ip);
            // ip2region 返回格式：国家|区域|省份|城市|ISP
            // 示例：中国|0|上海|上海|电信
            // 我们可以简化一下，只保留有意义的部分
            if (region != null) {
                region = region.replace("|0", "").replace("0|", "");
            }
            return region;
        } catch (Exception e) {
            log.warn("Failed to search IP region for {}: {}", ip, e.getMessage());
            return "未知";
        }
    }

    private static boolean isInternalIp(String ip) {
        if ("127.0.0.1".equals(ip) || "localhost".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        // 简单正则匹配私有 IP 地址范围
        // 10.0.0.0 - 10.255.255.255
        // 172.16.0.0 - 172.31.255.255
        // 192.168.0.0 - 192.168.255.255
        return ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                (ip.startsWith("172.") && isBetween(ip.split("\\.")[1], 16, 31));
    }

    private static boolean isBetween(String s, int min, int max) {
        try {
            int val = Integer.parseInt(s);
            return val >= min && val <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
