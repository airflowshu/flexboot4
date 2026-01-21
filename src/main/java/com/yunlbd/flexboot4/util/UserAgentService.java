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
        // 关键：为了准确性，必须将所有 Sec-CH-UA 请求头传递给解析器
        Map<String, String> requestHeaders = new HashMap<>();
        String header1 = request.getHeader("User-Agent");
        // // 传统的 User-Agent
        // requestHeaders.put("User-Agent", request.getHeader("User-Agent"));
        //
        // // 2026 年核心：Client Hints 相关头部
        // // Yauaa 会自动根据这些高熵信息修正 UA 冻结导致的版本误差
        // String[] chHeaders = {
        //         "Sec-CH-UA", "Sec-CH-UA-Mobile", "Sec-CH-UA-Platform",
        //         "Sec-CH-UA-Platform-Version", "Sec-CH-UA-Model", "Sec-CH-UA-Full-Version-List"
        // };
        //
        // for (String header : chHeaders) {
        //     requestHeaders.put(header, request.getHeader(header));
        // }

        // 执行解析
        UserAgent result = uaa.parse(header1);

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
