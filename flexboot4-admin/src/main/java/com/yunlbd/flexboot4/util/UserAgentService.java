package com.yunlbd.flexboot4.util;

import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
/**
 * User-Agent 解析工具类
 * 用于解析 HTTP 请求头中的 User-Agent 字符串，提取终端信息
 *
 * @author flexboot4
 * @since 2026-01-21
 */
@Service
public class UserAgentService {

    private final UserAgentAnalyzer uaa;

    public UserAgentService() {
        // 初始化解析器，建议作为单例或 Bean
        this.uaa = UserAgentAnalyzer
                .newBuilder()
                .withCache(10000) // 生产环境建议开启缓存
                .build();
    }

    public Map<String, String> parseRequest(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return parse(userAgent);
    }

    public Map<String, String> parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new HashMap<>();
        }
        // 执行解析
        UserAgent result = uaa.parse(userAgent);

        // 提取常用字段
        Map<String, String> info = new HashMap<>();
        info.put("deviceBrand", result.getValue("DeviceBrand"));       // 设备品牌 (Apple, Huawei)
        info.put("deviceCpu", result.getValue("DeviceCpu"));           // 设备架构
        info.put("deviceName", result.getValue("DeviceName"));         // 设备型号 (iPhone 16)
        info.put("osName", result.getValue("OperatingSystemName"));    // 操作系统 (HarmonyOS)
        info.put("osVersion", result.getValue("OperatingSystemVersion")); // 系统版本
        info.put("agentName", result.getValue("AgentName"));           // 浏览器 (Chrome)
        info.put("agentVersion", result.getValue("AgentVersion"));     // 浏览器版本

        return info;
    }
}
