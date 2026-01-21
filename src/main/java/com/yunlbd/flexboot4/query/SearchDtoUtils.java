package com.yunlbd.flexboot4.query;

import com.yunlbd.flexboot4.dto.SearchDto;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SearchDtoUtils {

    private SearchDtoUtils() {}

    public static SearchDto create(Integer pageNumber,
                                   Integer pageSize,
                                   String logic,
                                   List<SearchDto.SearchItem> items,
                                   List<SearchDto.OrderItem> orders) {
        SearchDto dto = new SearchDto();
        if (pageNumber != null) {
            dto.setPageNumber(pageNumber);
        }
        if (pageSize != null) {
            dto.setPageSize(pageSize);
        }
        if (logic != null && !logic.isBlank()) {
            dto.setLogic(logic);
        }
        dto.setItems(items);
        dto.setOrders(orders);
        return dto;
    }

    public static void filterRelationCollections(SearchDto dto, Class<?> rootEntityClass, List<?> records) {
        if (dto == null || rootEntityClass == null || records == null || records.isEmpty()) {
            return;
        }
        if (!hasRelationPaths(dto)) {
            return;
        }
        RelationQueryBuilder.RelationContext ctx = RelationQueryBuilder.prepare(rootEntityClass, dto);
        Map<String, List<LeafCondition>> relationConds = new LinkedHashMap<>();
        collectRelationConditions(ctx, dto.getItems(), relationConds);
        if (relationConds.isEmpty()) {
            return;
        }
        boolean andLogic = dto.getLogic() == null || "AND".equalsIgnoreCase(dto.getLogic().trim());
        for (Object root : records) {
            if (root == null) {
                continue;
            }
            for (var entry : relationConds.entrySet()) {
                String relKey = entry.getKey();
                Field relationField = findField(root.getClass(), relKey);
                if (relationField == null) {
                    continue;
                }
                relationField.setAccessible(true);
                Object relVal;
                try {
                    relVal = relationField.get(root);
                } catch (IllegalAccessException ignored) {
                    continue;
                }
                if (!(relVal instanceof List<?> list) || list.isEmpty()) {
                    continue;
                }
                List<?> filtered = filterList(list, entry.getValue(), andLogic);
                if (filtered == list) {
                    continue;
                }
                try {
                    relationField.set(root, filtered);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    public static boolean hasRelationPaths(SearchDto dto) {
        if (dto == null) return false;
        if (dto.getItems() != null) {
            for (SearchDto.SearchItem it : dto.getItems()) {
                if (containsRelationPath(it)) return true;
            }
        }
        if (dto.getOrders() != null) {
            for (SearchDto.OrderItem od : dto.getOrders()) {
                if (od.getColumn() != null && od.getColumn().contains(".")) return true;
            }
        }
        return false;
    }

    private static void collectRelationConditions(RelationQueryBuilder.RelationContext ctx,
                                                  List<SearchDto.SearchItem> items,
                                                  Map<String, List<LeafCondition>> out) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (SearchDto.SearchItem it : items) {
            if (it == null) {
                continue;
            }
            if (it.getChildren() != null && !it.getChildren().isEmpty()) {
                collectRelationConditions(ctx, it.getChildren(), out);
                continue;
            }
            String fieldPath = it.getField();
            String op = it.getOp();
            if (fieldPath == null || op == null || !fieldPath.contains(".")) {
                continue;
            }
            String[] parts = fieldPath.split("\\.");
            if (parts.length != 2) {
                continue;
            }
            String alias = parts[0];
            String prop = parts[1];
            String relKey = normalizeRel(ctx, alias);
            if (relKey == null || relKey.isEmpty()) {
                continue;
            }
            if (!ctx.relationFieldByName.containsKey(relKey)) {
                continue;
            }
            Object val = ValueConverter.convert(ctx, relKey + "." + prop, it.getVal());
            out.computeIfAbsent(relKey, k -> new ArrayList<>())
                    .add(new LeafCondition(prop, op.trim().toLowerCase(Locale.ROOT), val));
        }
    }

    private static String normalizeRel(RelationQueryBuilder.RelationContext ctx, String rel) {
        if (ctx.pathToTable.containsKey(rel)) {
            return rel;
        }
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

    private static Field findField(Class<?> clazz, String name) {
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

    private static List<?> filterList(List<?> list, List<LeafCondition> conditions, boolean andLogic) {
        if (conditions == null || conditions.isEmpty()) {
            return list;
        }
        List<Object> filtered = new ArrayList<>(list.size());
        boolean changed = false;
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            boolean matches = andLogic;
            for (LeafCondition c : conditions) {
                boolean ok = match(item, c);
                if (andLogic) {
                    if (!ok) {
                        matches = false;
                        break;
                    }
                } else {
                    if (ok) {
                        matches = true;
                        break;
                    }
                    matches = false;
                }
            }
            if (matches) {
                filtered.add(item);
            } else {
                changed = true;
            }
        }
        return changed ? filtered : list;
    }

    private static boolean match(Object bean, LeafCondition cond) {
        Object propVal = readProperty(bean, cond.prop());
        return switch (cond.op()) {
            case "eq" -> Objects.equals(propVal, cond.val());
            case "ne" -> !Objects.equals(propVal, cond.val());
            case "like" -> propVal != null && cond.val() != null && propVal.toString().contains(cond.val().toString());
            case "notlike" -> propVal == null || cond.val() == null || !propVal.toString().contains(cond.val().toString());
            case "isnull" -> propVal == null;
            case "notnull" -> propVal != null;
            case "in" -> {
                var coll = ValueConverter.toCollection(cond.val());
                yield coll != null && coll.contains(propVal);
            }
            case "notin" -> {
                var coll = ValueConverter.toCollection(cond.val());
                yield coll == null || !coll.contains(propVal);
            }
            case "gt", "ge", "lt", "le" -> compare(propVal, cond.val(), cond.op());
            default -> Objects.equals(propVal, cond.val());
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean compare(Object left, Object right, String op) {
        if (left == null || right == null) {
            return false;
        }
        if (left instanceof Comparable lc && right.getClass().isAssignableFrom(left.getClass())) {
            int c = lc.compareTo(right);
            return switch (op) {
                case "gt" -> c > 0;
                case "ge" -> c >= 0;
                case "lt" -> c < 0;
                case "le" -> c <= 0;
                default -> false;
            };
        }
        String ls = left.toString();
        String rs = right.toString();
        int c = ls.compareTo(rs);
        return switch (op) {
            case "gt" -> c > 0;
            case "ge" -> c >= 0;
            case "lt" -> c < 0;
            case "le" -> c <= 0;
            default -> false;
        };
    }

    private static Object readProperty(Object bean, String prop) {
        Field f = findField(bean.getClass(), prop);
        if (f == null) {
            return null;
        }
        f.setAccessible(true);
        try {
            return f.get(bean);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private record LeafCondition(String prop, String op, Object val) {
    }

    private static boolean containsRelationPath(SearchDto.SearchItem it) {
        if (it.getField() != null && it.getField().contains(".")) return true;
        if (it.getChildren() != null) {
            for (SearchDto.SearchItem c : it.getChildren()) {
                if (containsRelationPath(c)) return true;
            }
        }
        return false;
    }
}
