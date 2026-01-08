package com.yunlbd.flexboot4.query;

import java.lang.reflect.Field;
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
        String a = rel.toLowerCase(Locale.ROOT);
        for (var entry : ctx.pathToEntity.entrySet()) {
            Class<?> target = entry.getValue();
            String simple = target.getSimpleName().toLowerCase(Locale.ROOT);
            String table = RelationQueryBuilder.TableUtils.tableName(target).toLowerCase(Locale.ROOT);
            if (a.equals(simple) || a.equals(table)) {
                return entry.getKey();
            }
        }
        return rel;
    }
}
