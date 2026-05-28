package com.yunlbd.flexboot4.converter;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.RelationOneToOne;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BeanCopyUtils {

    private static final Set<String> SYSTEM_FIELDS = Set.of(
            "id",
            "version",
            "delFlag",
            "createTime",
            "lastModifyTime",
            "createBy",
            "lastModifyBy"
    );

    private BeanCopyUtils() {
    }

    public static void copyNonNullProperties(Object source, Object target) {
        if (source == null || target == null) {
            return;
        }
        Map<String, Field> targetFields = fieldMap(target.getClass());
        Class<?> current = source.getClass();
        while (current != null && current != Object.class) {
            for (Field sourceField : current.getDeclaredFields()) {
                if (shouldSkip(sourceField)) {
                    continue;
                }
                Field targetField = targetFields.get(sourceField.getName());
                if (targetField == null || shouldSkip(targetField)) {
                    continue;
                }
                if (!targetField.getType().isAssignableFrom(sourceField.getType())) {
                    continue;
                }
                try {
                    sourceField.setAccessible(true);
                    Object value = sourceField.get(source);
                    if (value == null) {
                        continue;
                    }
                    targetField.setAccessible(true);
                    targetField.set(target, value);
                } catch (IllegalAccessException ignored) {
                }
            }
            current = current.getSuperclass();
        }
    }

    private static Map<String, Field> fieldMap(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.putIfAbsent(field.getName(), field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
            return true;
        }
        if (SYSTEM_FIELDS.contains(field.getName())) {
            return true;
        }
        if (field.isAnnotationPresent(Id.class)
                || field.isAnnotationPresent(RelationManyToOne.class)
                || field.isAnnotationPresent(RelationOneToOne.class)
                || field.isAnnotationPresent(RelationOneToMany.class)
                || field.isAnnotationPresent(RelationManyToMany.class)) {
            return true;
        }
        Column column = field.getAnnotation(Column.class);
        return column != null && column.ignore();
    }
}
