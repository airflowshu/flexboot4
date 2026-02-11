package com.yunlbd.flexboot4.util;

import java.util.*;

public final class LogDesensitizationUtils {

    private LogDesensitizationUtils() {
    }

    public static Map<String, Object> desensitize(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        Object r = sanitizeValue(null, data);
        if (r instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> out = (Map<String, Object>) m;
            return out;
        }
        return data;
    }

    private static Object sanitizeValue(String key, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> out = new ArrayList<>(collection.size());
            for (Object it : collection) {
                out.add(sanitizeValue(null, it));
            }
            return out;
        }
        if (value instanceof Object[] arr) {
            List<Object> out = new ArrayList<>(arr.length);
            for (Object it : arr) {
                out.add(sanitizeValue(null, it));
            }
            return out;
        }
        if (value instanceof String s) {
            return maskByKey(key, s);
        }
        return value;
    }

    private static Map<String, Object> sanitizeMap(Map<?, ?> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String k = String.valueOf(e.getKey());
            Object v = e.getValue();
            out.put(k, sanitizeValue(k, v));
        }
        return out;
    }

    private static String maskByKey(String key, String value) {
        if (value == null || value.isBlank() || key == null) {
            return value;
        }
        String k = normalizeKey(key);
        if (containsAny(k, "password", "pwd", "secret")) {
            return "******";
        }
        if (containsAny(k, "phone", "mobile")) {
            return maskPhone(value);
        }
        if (containsAny(k, "tel", "telephone", "fax")) {
            return maskFixedPhone(value);
        }
        if (containsAny(k, "idcard", "identity", "certno")) {
            return maskIdCard(value);
        }
        if (containsAny(k, "email", "mail")) {
            return maskEmail(value);
        }
        if (containsAny(k, "bankcard", "cardno", "creditcard", "acctno")) {
            return maskBankCard(value);
        }
        if (containsAny(k, "address", "addr", "location", "province", "city", "district", "street")) {
            return maskAddress(value);
        }
        if (containsAny(k, "carno", "plateno", "plate")) {
            return maskCarPlate(value);
        }
        if (containsAny(k, "token", "accesstoken", "jwt")) {
            return maskToken(value);
        }
        return value;
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase().replace("_", "");
    }

    private static boolean containsAny(String key, String... keywords) {
        for (String kw : keywords) {
            if (key.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank() || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String maskFixedPhone(String phone) {
        if (phone == null || phone.isBlank() || phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }

    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank() || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "**********" + idCard.substring(idCard.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.substring(0, 1) + "****" + email.substring(atIndex);
    }

    public static String maskBankCard(String cardNo) {
        if (cardNo == null || cardNo.isBlank() || cardNo.length() < 10) {
            return cardNo;
        }
        return cardNo.substring(0, 6) + "******" + cardNo.substring(cardNo.length() - 4);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank() || address.length() < 6) {
            return address;
        }
        return address.substring(0, 6) + "******";
    }

    public static String maskCarPlate(String carNo) {
        if (carNo == null || carNo.isBlank() || carNo.length() < 4) {
            return carNo;
        }
        return carNo.substring(0, 2) + "***" + carNo.substring(carNo.length() - 2);
    }

    public static String maskToken(String token) {
        if (token == null || token.isBlank() || token.length() <= 10) {
            return token;
        }
        return token.substring(0, 10) + "******";
    }
}

