package com.yunlbd.flexboot4.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class AiApiKeyGenerator {

    private static final String PREFIX = "yl-";
    // private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private AiApiKeyGenerator() {
    }

    /**
     * 生成格式为 yl-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx 的随机 API Key
     * 前缀 yl- 加上 32 位随机字符
     *
     * @return 生成的 API Key
     */
    public static String createKey() {
        StringBuilder sb = new StringBuilder(PREFIX);
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // 取 32 位字符
        sb.append(encoded, 0, 32);
        return sb.toString();
    }
}
