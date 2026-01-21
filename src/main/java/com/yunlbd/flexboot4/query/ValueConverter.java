package com.yunlbd.flexboot4.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ValueConverter {
    public static Object convert(RelationQueryBuilder.RelationContext ctx, String fieldPath, Object val) {
        if (val == null) return null;
        Class<?> type = propertyType(ctx, fieldPath);
        if (type == null) return val;
        try {
            if (type == java.time.LocalDateTime.class) {
                if (val instanceof java.time.LocalDateTime) return val;
                String s = String.valueOf(val).trim();
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return java.time.LocalDateTime.parse(s, fmt);
            } else if (type == java.time.LocalDate.class) {
                if (val instanceof java.time.LocalDate) return val;
                String s = String.valueOf(val).trim();
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return java.time.LocalDate.parse(s, fmt);
            } else if (type == Integer.class || type == int.class) {
                return Integer.valueOf(String.valueOf(val));
            } else if (type == Long.class || type == long.class) {
                return Long.valueOf(String.valueOf(val));
            } else if (type == Boolean.class || type == boolean.class) {
                String s = String.valueOf(val).trim();
                if ("1".equals(s)) return true;
                if ("0".equals(s)) return false;
                return Boolean.valueOf(s);
            } else if (type.isEnum()) {
                String s = String.valueOf(val).trim();
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Object e = Enum.valueOf((Class<Enum>) type, s);
                return e;
            }
        } catch (Exception ignore) {
            return val;
        }
        return val;
    }

    public static Collection<?> toCollection(Object val) {
        if (val == null) return null;
        if (val instanceof Collection<?>) return (Collection<?>) val;
        if (val.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(val);
            List<Object> list = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                list.add(java.lang.reflect.Array.get(val, i));
            }
            return list;
        }
        String s = String.valueOf(val);
        String[] arr = s.split(",");
        return Arrays.stream(arr).map(String::trim).filter(v -> !v.isEmpty()).toList();
    }

    public static Class<?> propertyType(RelationQueryBuilder.RelationContext ctx, String fieldPath) {
        try {
            if (fieldPath == null || fieldPath.isEmpty()) return null;
            String prop;
            Class<?> owner;
            if (fieldPath.contains(".")) {
                String[] parts = fieldPath.split("\\.");
                String relKey = resolveRelKey(ctx, parts[0]);
                owner = ctx.pathToEntity.get(relKey);
                prop = parts[1];
            } else {
                owner = ctx.rootEntity;
                prop = fieldPath;
            }
            if (owner == null) return null;
            java.lang.reflect.Field f = findField(owner, prop);
            if (f != null) return f.getType();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignore) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static String resolveRelKey(RelationQueryBuilder.RelationContext ctx, String alias) {
        if (ctx == null) {
            return alias;
        }
        if (alias == null) {
            return alias;
        }
        if (ctx.pathToEntity.containsKey(alias)) {
            return alias;
        }
        String a = normalizeKey(alias);
        String rootSimpleCamel = normalizeKey(lowerCamelFromSimpleName(ctx.rootEntity != null ? ctx.rootEntity.getSimpleName() : null));
        String rootTableCamel = normalizeKey(lowerCamelFromTableName(ctx.rootTable));
        if (!a.isEmpty() && (a.equals(rootSimpleCamel) || a.equals(rootTableCamel))) {
            return "";
        }
        for (var entry : ctx.pathToEntity.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            Class<?> target = entry.getValue();
            String keyNorm = normalizeKey(key);
            String simpleCamel = normalizeKey(lowerCamelFromSimpleName(target != null ? target.getSimpleName() : null));
            String tableCamel = normalizeKey(lowerCamelFromTableName(target != null ? RelationQueryBuilder.TableUtils.tableName(target) : null));
            if (a.equals(keyNorm)
                    || a.equals(simpleCamel)
                    || a.equals(tableCamel)
            ) {
                return key;
            }
        }
        return alias;
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String lowerCamelFromSimpleName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            return "";
        }
        char first = simpleName.charAt(0);
        if (Character.isLowerCase(first)) {
            return simpleName;
        }
        return Character.toLowerCase(first) + simpleName.substring(1);
    }

    private static String lowerCamelFromTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return "";
        }
        String t = tableName.trim();
        int dot = t.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < t.length()) {
            t = t.substring(dot + 1);
        }
        StringBuilder sb = new StringBuilder(t.length());
        boolean upperNext = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '_' || ch == '-' || ch == ' ') {
                upperNext = true;
                continue;
            }
            if (sb.isEmpty()) {
                sb.append(Character.toLowerCase(ch));
                upperNext = false;
                continue;
            }
            sb.append(upperNext ? Character.toUpperCase(ch) : ch);
            upperNext = false;
        }
        return sb.toString();
    }
}
