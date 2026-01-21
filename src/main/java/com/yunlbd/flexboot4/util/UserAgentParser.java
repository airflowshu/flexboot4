package com.yunlbd.flexboot4.util;

import lombok.extern.slf4j.Slf4j;

/**
 * User-Agent 解析工具类
 * 用于解析 HTTP 请求头中的 User-Agent 字符串，提取终端信息
 *
 * @author flexboot4
 * @since 2026-01-21
 */
@Slf4j
public class UserAgentParser {

    private UserAgentParser() {
        // 工具类私有构造器
    }

    /**
     * 解析 User-Agent 字符串，提取终端信息
     *
     * @param userAgent User-Agent 字符串
     * @return 格式化的终端信息，如 "Windows 10 - Chrome 120"
     */
    public static String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown Terminal";
        }

        try {
            StringBuilder terminal = new StringBuilder();

            // 检测操作系统
            String os = detectOperatingSystem(userAgent);
            terminal.append(os);

            // 检测浏览器
            String browser = detectBrowser(userAgent);
            terminal.append(" - ").append(browser);

            // 检测设备类型（移动端）
            String deviceInfo = detectDeviceInfo(userAgent);
            if (!deviceInfo.isEmpty()) {
                terminal.append(deviceInfo);
            }

            return terminal.toString();
        } catch (Exception e) {
            log.warn("Failed to parse User-Agent: {}", userAgent, e);
            return "Unknown Terminal";
        }
    }

    /**
     * 检测操作系统
     */
    private static String detectOperatingSystem(String userAgent) {
        if (userAgent.contains("Windows 10") || userAgent.contains("Windows NT 10.0")) {
            return "Windows 10";
        } else if (userAgent.contains("Windows 11")) {
            return "Windows 11";
        } else if (userAgent.contains("Windows NT 6.1")) {
            return "Windows 7";
        } else if (userAgent.contains("Windows NT 6.3")) {
            return "Windows 8.1";
        } else if (userAgent.contains("Windows NT 6.2")) {
            return "Windows 8";
        } else if (userAgent.contains("Mac OS X")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iOS")) {
            return "iOS";
        }
        return "Unknown OS";
    }

    /**
     * 检测浏览器
     */
    private static String detectBrowser(String userAgent) {
        if (userAgent.contains("Edg/")) {
            String version = extractVersion(userAgent, "Edg/");
            return "Microsoft Edge " + version;
        } else if (userAgent.contains("Chrome/")) {
            String version = extractVersion(userAgent, "Chrome/");
            return "Chrome " + version;
        } else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) {
            String version = extractVersion(userAgent, "Version/");
            return "Safari " + version;
        } else if (userAgent.contains("Firefox/")) {
            String version = extractVersion(userAgent, "Firefox/");
            return "Firefox " + version;
        } else if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "Internet Explorer";
        }
        return "Unknown Browser";
    }

    /**
     * 检测设备信息（移动端）
     */
    private static String detectDeviceInfo(String userAgent) {
        if (userAgent.contains("Mobile")) {
            if (userAgent.contains("Android")) {
                String model = extractMobileModel(userAgent);
                if (!model.isEmpty()) {
                    return " (" + model + ")";
                }
                return " (Mobile)";
            } else if (userAgent.contains("iPhone")) {
                return " (iPhone)";
            } else if (userAgent.contains("iPad")) {
                return " (iPad)";
            } else {
                return " (Mobile)";
            }
        }
        return "";
    }

    /**
     * 提取版本号
     */
    private static String extractVersion(String userAgent, String prefix) {
        int index = userAgent.indexOf(prefix);
        if (index == -1) {
            return "";
        }
        index += prefix.length();
        int endIndex = userAgent.indexOf(" ", index);
        if (endIndex == -1) {
            endIndex = userAgent.length();
        }
        return userAgent.substring(index, endIndex);
    }

    /**
     * 提取移动设备型号
     */
    private static String extractMobileModel(String userAgent) {
        // 尝试提取Android设备型号
        int androidIndex = userAgent.indexOf("Android");
        if (androidIndex != -1) {
            androidIndex += "Android".length();
            int endIndex = userAgent.indexOf(";", androidIndex);
            if (endIndex == -1) {
                endIndex = userAgent.indexOf(")", androidIndex);
            }
            if (endIndex != -1) {
                return userAgent.substring(androidIndex, endIndex).trim();
            }
        }
        return "";
    }
}
