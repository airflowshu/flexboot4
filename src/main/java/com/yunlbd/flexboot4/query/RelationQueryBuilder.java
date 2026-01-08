package com.yunlbd.flexboot4.query;

import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class RelationQueryBuilder {

    public static class RelationContext {
        public Class<?> rootEntity;
        public String rootTable;
        public final Map<String, String> pathToTable = new LinkedHashMap<>();
        public final Map<String, Class<?>> pathToEntity = new LinkedHashMap<>();
        public final Map<String, Field> relationFieldByName = new LinkedHashMap<>();
    }

    public static <T> RelationContext prepare(Class<T> entityClass, SearchDto dto) {
        RelationContext ctx = new RelationContext();
        ctx.rootEntity = entityClass;
        ctx.rootTable = TableUtils.tableName(entityClass);
        ctx.pathToTable.put("", ctx.rootTable);
        ctx.pathToEntity.put("", entityClass);
        Set<String> paths = collectPaths(dto);
        Map<String, Field> relationFields = relationFieldMap(entityClass);
        for (String path : paths) {
            String top = topPath(path);
            Field f = relationFields.get(top);
            if (f == null) {
                f = matchRelationByAlias(relationFields, top);
                if (f != null) {
                    top = f.getName();
                }
            }
            if (f == null) {
                continue;
            }
            Class<?> target = targetEntity(f);
            if (target == null) {
                continue;
            }
            String targetTable = TableUtils.tableName(target);
            ctx.pathToTable.put(top, targetTable);
            ctx.pathToEntity.put(top, target);
            ctx.relationFieldByName.put(top, f);
        }
        return ctx;
    }

    public static void buildJoins(QueryWrapper qw, RelationContext ctx) {
        Map<String, Field> relationFields = relationFieldMap(ctx.rootEntity);
        for (Map.Entry<String, String> e : ctx.pathToTable.entrySet()) {
            String path = e.getKey();
            if (path.isEmpty()) {
                continue;
            }
            Field f = relationFields.get(path);
            if (f == null) {
                continue;
            }
            RelationManyToOne mto = f.getAnnotation(RelationManyToOne.class);
            if (mto != null) {
                String targetTable = TableUtils.tableName(targetEntity(f));
                String left = targetTable + "." + TableUtils.columnName(mto.targetField());
                String right = ctx.rootTable + "." + TableUtils.columnName(mto.selfField());
                qw.leftJoin(targetTable).on(left + " = " + right);
                continue;
            }
            RelationManyToMany mtm = f.getAnnotation(RelationManyToMany.class);
            if (mtm != null) {
                String joinTable = mtm.joinTable();
                String targetTable = TableUtils.tableName(targetEntity(f));
                String sLeft = joinTable + "." + mtm.joinSelfColumn();
                String sRight = ctx.rootTable + "." + TableUtils.columnName(mtm.selfField());
                String tLeft = targetTable + "." + TableUtils.columnName(mtm.targetField());
                String tRight = joinTable + "." + mtm.joinTargetColumn();
                qw.leftJoin(joinTable).on(sLeft + " = " + sRight);
                qw.leftJoin(targetTable).on(tLeft + " = " + tRight);
            }
        }
    }

    private static Set<String> collectPaths(SearchDto dto) {
        Set<String> set = new LinkedHashSet<>();
        if (dto == null) return set;
        if (dto.getItems() != null) {
            for (SearchDto.SearchItem it : dto.getItems()) {
                collectItemPaths(it, set);
            }
        }
        if (dto.getOrders() != null) {
            for (SearchDto.OrderItem od : dto.getOrders()) {
                if (od.getColumn() != null && od.getColumn().contains(".")) {
                    set.add(od.getColumn().split("\\.")[0]);
                }
            }
        }
        return set;
    }

    private static void collectItemPaths(SearchDto.SearchItem it, Set<String> set) {
        if (it.getField() != null && it.getField().contains(".")) {
            set.add(it.getField().split("\\.")[0]);
        }
        if (it.getChildren() != null) {
            for (SearchDto.SearchItem c : it.getChildren()) {
                collectItemPaths(c, set);
            }
        }
    }

    private static Map<String, Field> relationFieldMap(Class<?> entityClass) {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.getAnnotation(RelationManyToOne.class) != null || f.getAnnotation(RelationManyToMany.class) != null) {
                map.put(f.getName(), f);
            }
        }
        return map;
    }

    private static Class<?> targetEntity(Field f) {
        RelationManyToOne mto = f.getAnnotation(RelationManyToOne.class);
        if (mto != null) {
            return f.getType();
        }
        RelationManyToMany mtm = f.getAnnotation(RelationManyToMany.class);
        if (mtm != null) {
            if (List.class.isAssignableFrom(f.getType())) {
                return GenericUtils.resolveGenericType(f);
            }
        }
        return null;
    }

    private static String topPath(String path) {
        int i = path.indexOf('.');
        return i > 0 ? path.substring(0, i) : path;
    }

    private static Field matchRelationByAlias(Map<String, Field> relationFields, String alias) {
        String a = alias.toLowerCase(Locale.ROOT);
        for (Field f : relationFields.values()) {
            Class<?> target = targetEntity(f);
            if (target == null) continue;
            String simple = target.getSimpleName().toLowerCase(Locale.ROOT);
            String table = TableUtils.tableName(target).toLowerCase(Locale.ROOT);
            if (a.equals(simple) || a.equals(table)) {
                return f;
            }
        }
        return null;
    }

    public static class TableUtils {
        public static String tableName(Class<?> entityClass) {
            com.mybatisflex.annotation.Table t = entityClass.getAnnotation(com.mybatisflex.annotation.Table.class);
            if (t != null && t.value() != null && !t.value().isEmpty()) {
                return t.value();
            }
            return entityClass.getSimpleName();
        }
        public static String columnName(String property) {
            return CaseUtils.camelToUnderscore(property);
        }
    }

    public static class GenericUtils {
        public static Class<?> resolveGenericType(Field f) {
            try {
                java.lang.reflect.Type gt = f.getGenericType();
                if (gt instanceof java.lang.reflect.ParameterizedType pt) {
                    java.lang.reflect.Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0 && args[0] instanceof Class<?> c) {
                        return c;
                    }
                }
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    public static class CaseUtils {
        public static String camelToUnderscore(String name) {
            if (name == null || name.isEmpty()) return name;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < name.length(); i++) {
                char ch = name.charAt(i);
                if (Character.isUpperCase(ch)) {
                    sb.append('_').append(Character.toLowerCase(ch));
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
    }
}
