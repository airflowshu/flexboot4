package com.yunlbd.flexboot4.query;

import java.util.Locale;

public class FieldResolver {
    public static String resolveColumn(RelationQueryBuilder.RelationContext ctx, String fieldPath) {
        if (fieldPath == null || fieldPath.isEmpty()) return null;
        if (fieldPath.contains(".")) {
            String[] parts = fieldPath.split("\\.");
            String rel = normalizeRel(ctx, parts[0]);
            String prop = parts[1];
            String table = ctx.pathToTable.getOrDefault(rel, null);
            if (table == null) return null;
            return table + "." + RelationQueryBuilder.TableUtils.columnName(prop);
        } else {
            String table = ctx.rootTable;
            return table + "." + RelationQueryBuilder.TableUtils.columnName(fieldPath);
        }
    }

    private static String normalizeRel(RelationQueryBuilder.RelationContext ctx, String rel) {
        if (ctx.pathToTable.containsKey(rel)) return rel;
        String a = normalizeKey(rel);
        String rootSimpleCamel = normalizeKey(lowerCamelFromSimpleName(ctx.rootEntity.getSimpleName()));
        String rootTableCamel = normalizeKey(lowerCamelFromTableName(ctx.rootTable));
        if (a.equals(rootSimpleCamel) || a.equals(rootTableCamel)) {
            return "";
        }
        for (var entry : ctx.pathToEntity.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            Class<?> target = entry.getValue();
            String keyNorm = normalizeKey(key);
            String simpleCamel = normalizeKey(lowerCamelFromSimpleName(target.getSimpleName()));
            String tableCamel = normalizeKey(lowerCamelFromTableName(RelationQueryBuilder.TableUtils.tableName(target)));
            if (a.equals(keyNorm)
                    || a.equals(simpleCamel)
                    || a.equals(tableCamel)
            ) {
                return key;
            }
        }
        return rel;
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
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
