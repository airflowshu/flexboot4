package com.yunlbd.flexboot4.controller.sys;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.RelationOneToOne;
import com.yunlbd.flexboot4.dto.SearchDto;
import com.yunlbd.flexboot4.query.RelationQueryBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record CrudFieldPolicy(Set<String> queryFields, Set<String> orderFields) {

    public static CrudFieldPolicy of(Collection<String> queryFields, Collection<String> orderFields) {
        return new CrudFieldPolicy(copy(queryFields), copy(orderFields));
    }

    public static CrudFieldPolicy same(Collection<String> fields) {
        Set<String> copied = copy(fields);
        return new CrudFieldPolicy(copied, copied);
    }

    public static CrudFieldPolicy entitySimpleFields(Class<?> entityClass) {
        return same(simpleFieldNames(entityClass));
    }

    public CrudFieldPolicy withQueryFields(String... fields) {
        Set<String> merged = new LinkedHashSet<>(queryFields);
        if (fields != null) {
            for (String field : fields) {
                if (field != null && !field.isBlank()) {
                    merged.add(field.trim());
                }
            }
        }
        return new CrudFieldPolicy(Set.copyOf(merged), orderFields);
    }

    public CrudFieldPolicy withOrderFields(String... fields) {
        Set<String> merged = new LinkedHashSet<>(orderFields);
        if (fields != null) {
            for (String field : fields) {
                if (field != null && !field.isBlank()) {
                    merged.add(field.trim());
                }
            }
        }
        return new CrudFieldPolicy(queryFields, Set.copyOf(merged));
    }

    public void validate(SearchDto searchDto) {
        validate(searchDto, null);
    }

    public void validate(SearchDto searchDto, Class<?> rootEntityClass) {
        if (searchDto == null) {
            return;
        }
        RelationQueryBuilder.RelationContext ctx = rootEntityClass == null
                ? null
                : RelationQueryBuilder.prepare(rootEntityClass, searchDto);
        validateItems(searchDto.getItems(), ctx);
        if (searchDto.getOrders() != null) {
            for (SearchDto.OrderItem order : searchDto.getOrders()) {
                if (order == null || order.getColumn() == null || order.getColumn().isBlank()) {
                    continue;
                }
                String column = order.getColumn().trim();
                if (!isAllowed(column, orderFields, ctx)) {
                    throw new IllegalArgumentException("排序字段不允许: " + column);
                }
            }
        }
    }

    private void validateItems(List<SearchDto.SearchItem> items, RelationQueryBuilder.RelationContext ctx) {
        if (items == null) {
            return;
        }
        for (SearchDto.SearchItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                validateItems(item.getChildren(), ctx);
                continue;
            }
            if (item.getField() == null || item.getField().isBlank()) {
                continue;
            }
            String field = item.getField().trim();
            if (!isAllowed(field, queryFields, ctx)) {
                throw new IllegalArgumentException("查询字段不允许: " + field);
            }
        }
    }

    private static boolean isAllowed(String fieldPath, Set<String> allowedFields,
                                     RelationQueryBuilder.RelationContext ctx) {
        if (allowedFields.contains(fieldPath)) {
            return true;
        }
        String canonical = canonicalFieldPath(fieldPath, ctx);
        return canonical != null && allowedFields.contains(canonical);
    }

    private static String canonicalFieldPath(String fieldPath, RelationQueryBuilder.RelationContext ctx) {
        if (ctx == null || fieldPath == null || !fieldPath.contains(".")) {
            return fieldPath;
        }
        String[] parts = fieldPath.split("\\.", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        String pathKey = resolvePathKey(ctx, parts[0]);
        if (pathKey == null) {
            return null;
        }
        return pathKey.isEmpty() ? parts[1] : pathKey + "." + parts[1];
    }

    private static String resolvePathKey(RelationQueryBuilder.RelationContext ctx, String alias) {
        if (ctx.pathToEntity.containsKey(alias)) {
            return alias;
        }
        String normalizedAlias = normalizeKey(alias);
        String rootSimpleCamel = normalizeKey(lowerCamelFromSimpleName(ctx.rootEntity.getSimpleName()));
        String rootTableCamel = normalizeKey(lowerCamelFromTableName(ctx.rootTable));
        if (!normalizedAlias.isEmpty()
                && (normalizedAlias.equals(rootSimpleCamel) || normalizedAlias.equals(rootTableCamel))) {
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
            if (normalizedAlias.equals(keyNorm)
                    || normalizedAlias.equals(simpleCamel)
                    || normalizedAlias.equals(tableCamel)) {
                return key;
            }
        }
        return null;
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

    private static Set<String> simpleFieldNames(Class<?> entityClass) {
        Set<String> fields = new LinkedHashSet<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (isAllowedSimpleField(field)) {
                    fields.add(field.getName());
                }
            }
            current = current.getSuperclass();
        }
        return Set.copyOf(fields);
    }

    private static boolean isAllowedSimpleField(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
            return false;
        }
        if (field.isAnnotationPresent(RelationManyToOne.class)
                || field.isAnnotationPresent(RelationOneToOne.class)
                || field.isAnnotationPresent(RelationOneToMany.class)
                || field.isAnnotationPresent(RelationManyToMany.class)) {
            return false;
        }
        Column column = field.getAnnotation(Column.class);
        if (column != null && column.ignore()) {
            return false;
        }
        return !field.getName().equals("delFlag");
    }

    private static Set<String> copy(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> copied = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                copied.add(value.trim());
            }
        }
        return Set.copyOf(copied);
    }
}
