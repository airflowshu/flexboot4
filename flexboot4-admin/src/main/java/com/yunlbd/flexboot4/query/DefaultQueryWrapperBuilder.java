package com.yunlbd.flexboot4.query;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Slf4j
public class DefaultQueryWrapperBuilder extends AbstractQueryWrapperBuilder {

    private static final DefaultQueryWrapperBuilder INSTANCE = new DefaultQueryWrapperBuilder();

    public static DefaultQueryWrapperBuilder get() {
        return INSTANCE;
    }

    @Override
    public QueryWrapper build(SearchDto dto, Class<?> entityClass) {
        return build(dto, entityClass, null);
    }

    /**
     * 构建QueryWrapper，支持指定具体表名
     * @param dto 搜索条件
     * @param entityClass 实体类
     * @param tableName 指定表名，如果为null则使用实体类的默认表名
     * @return QueryWrapper
     */
    public QueryWrapper build(SearchDto dto, Class<?> entityClass, String tableName) {
        QueryWrapper qw;
        if (tableName != null && !tableName.isEmpty()) {
            qw = QueryWrapper.create().from(tableName);
        } else {
            qw = QueryWrapper.create().from(entityClass);
        }
        RelationQueryBuilder.RelationContext ctx = RelationQueryBuilder.prepare(entityClass, dto);
        if (tableName != null && !tableName.isEmpty()) {
            ctx.rootTable = tableName;
            ctx.pathToTable.put("", tableName);
        }
        // log.info("=== Debug QueryWrapper Build ===");
        // log.info("entityClass: {}", entityClass.getSimpleName());
        // log.info("rootTable: {}", ctx.rootTable);
        // log.info("pathToTable: {}", ctx.pathToTable);
        // log.info("hasRelationPaths: {}", SearchDtoUtils.hasRelationPaths(dto));

        RelationQueryBuilder.buildJoins(qw, ctx);
        List<SearchDto.SearchItem> items = dto.getItems() == null ? Collections.emptyList() : dto.getItems();
        String rootLogic = dto.getLogic() == null ? "AND" : dto.getLogic().trim().toUpperCase(Locale.ROOT);
        boolean relationPresent = SearchDtoUtils.hasRelationPaths(dto);
        for (SearchDto.SearchItem it : items) {
            if (relationPresent && it.getField() != null && !it.getField().contains(".") && (it.getChildren() == null || it.getChildren().isEmpty())) {
                throw new IllegalArgumentException("Ambiguous field '" + it.getField() + "' in relation query. Use table prefix like 'dept.status' or 'roles.createTime'.");
            }
            applyItem(qw, ctx, it, rootLogic);
        }
        List<SearchDto.OrderItem> orders = dto.getOrders() == null ? Collections.emptyList() : dto.getOrders();
        for (SearchDto.OrderItem od : orders) {
            if (relationPresent && od.getColumn() != null && !od.getColumn().contains(".")) {
                throw new IllegalArgumentException("Ambiguous order column '" + od.getColumn() + "' in relation query. Use table prefix like 'dept.id' or 'roles.createTime'.");
            }
            String col = FieldResolver.resolveColumn(ctx, od.getColumn());
            if (col != null && !col.isEmpty()) {
                qw.orderBy(col, od.isAsc());
            }
        }
        return qw;
    }

    private void applyItem(QueryWrapper qw, RelationQueryBuilder.RelationContext ctx, SearchDto.SearchItem it, String groupLogic) {
        boolean isAnd = "AND".equalsIgnoreCase(groupLogic);
        if (it.getChildren() != null && !it.getChildren().isEmpty()) {
            log.info("Processing item with children, groupLogic: {}, childrenLogic: {}", groupLogic, it.getLogic());
            if (isAnd) {
                qw.and((java.util.function.Consumer<QueryWrapper>) w -> applyChildren(w, ctx, it.getChildren(), it.getLogic()));
            } else {
                qw.or((java.util.function.Consumer<QueryWrapper>) w -> applyChildren(w, ctx, it.getChildren(), it.getLogic()));
            }
            return;
        }
        if (it.getField() == null || it.getOp() == null) {
            log.info("Skipping item: field={}, op={}", it.getField(), it.getOp());
            return;
        }
        String field = it.getField();
        String op = it.getOp().trim().toLowerCase(Locale.ROOT);
        Object val = ValueConverter.convert(ctx, field, it.getVal());
        log.info("Processing field: {}, op: {}, val: {}, isAnd: {}", field, op, val, isAnd);
        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            String alias = parts[0];
            String prop = parts[1];
            String rel = normalizeRel(ctx, alias);
            String targetTable = ctx.pathToTable.get(rel);
            log.info("Field contains dot: alias={}, rel={}, targetTable={}", alias, rel, targetTable);
            if (targetTable == null) {
                throw new IllegalArgumentException("Unknown relation path '" + alias + "' in field '" + field + "'. Use lowerCamel entity/table name like 'sysDictType.code' or relation field name like 'dictItems.itemCode'.");
            }
            if (rel.isEmpty()) {
                String col = ctx.rootTable + "." + RelationQueryBuilder.TableUtils.columnName(prop);
                log.info("Matched as root entity field, applying condition: {}", col);
                if (isAnd) {
                    qw.and((java.util.function.Consumer<QueryWrapper>) wrapper -> OperatorStrategies.applyInto(wrapper, col, op, val));
                } else {
                    qw.or((java.util.function.Consumer<QueryWrapper>) wrapper -> OperatorStrategies.applyInto(wrapper, col, op, val));
                }
                return;
            }
            String existsSql = buildExistsSql(ctx, rel, targetTable, prop, op);
            Object param = val;
            if (existsSql == null) return;
            if (isAnd) {
                qw.and((java.util.function.Consumer<QueryWrapper>) w -> w.where(existsSql, param));
            } else {
                qw.or((java.util.function.Consumer<QueryWrapper>) w -> w.where(existsSql, param));
            }
        } else {
            String col = FieldResolver.resolveColumn(ctx, field);
            if (col == null || col.isEmpty()) {
                return;
            }
            if (isAnd) {
                qw.and((java.util.function.Consumer<QueryWrapper>) wrapper -> OperatorStrategies.applyInto(wrapper, col, op, val));
            } else {
                qw.or((java.util.function.Consumer<QueryWrapper>) wrapper -> OperatorStrategies.applyInto(wrapper, col, op, val));
            }
        }
    }

    private void applyChildren(QueryWrapper w, RelationQueryBuilder.RelationContext ctx, List<SearchDto.SearchItem> children, String logic) {
        String childLogic = logic == null ? "AND" : logic.trim().toUpperCase(Locale.ROOT);
        for (SearchDto.SearchItem child : children) {
            applyItem(w, ctx, child, childLogic);
        }
    }

    private String normalizeRel(RelationQueryBuilder.RelationContext ctx, String rel) {
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

    private String normalizeKey(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private String lowerCamelFromSimpleName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            return "";
        }
        char first = simpleName.charAt(0);
        if (Character.isLowerCase(first)) {
            return simpleName;
        }
        return Character.toLowerCase(first) + simpleName.substring(1);
    }

    private String lowerCamelFromTableName(String tableName) {
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

    private String buildExistsSql(RelationQueryBuilder.RelationContext ctx, String rel, String targetTable, String prop, String op) {
        java.lang.reflect.Field rf = ctx.relationFieldByName.get(rel);
        if (rf == null) return null;
        String root = ctx.rootTable;
        com.mybatisflex.annotation.RelationManyToOne mto = rf.getAnnotation(com.mybatisflex.annotation.RelationManyToOne.class);
        if (mto != null) {
            String left = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(mto.targetField()) + "\"";
            String right = "\"" + root + "\".\"" + RelationQueryBuilder.TableUtils.columnName(mto.selfField()) + "\"";
            String tcol = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(prop) + "\"";
            return "exists (select 1 from \"" + targetTable + "\" where " + left + " = " + right + " and " + tcol + " " + opToSql(op) + " ?)";
        }
        com.mybatisflex.annotation.RelationOneToMany otm = rf.getAnnotation(com.mybatisflex.annotation.RelationOneToMany.class);
        if (otm != null) {
            String left = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(otm.targetField()) + "\"";
            String right = "\"" + root + "\".\"" + RelationQueryBuilder.TableUtils.columnName(otm.selfField()) + "\"";
            String tcol = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(prop) + "\"";
            return "exists (select 1 from \"" + targetTable + "\" where " + left + " = " + right + " and " + tcol + " " + opToSql(op) + " ?)";
        }
        com.mybatisflex.annotation.RelationManyToMany mtm = rf.getAnnotation(com.mybatisflex.annotation.RelationManyToMany.class);
        if (mtm != null) {
            String join = mtm.joinTable();
            String sLeft = "\"" + join + "\".\"" + mtm.joinSelfColumn() + "\"";
            String sRight = "\"" + root + "\".\"" + RelationQueryBuilder.TableUtils.columnName(mtm.selfField()) + "\"";
            String tLeft = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(mtm.targetField()) + "\"";
            String tRight = "\"" + join + "\".\"" + mtm.joinTargetColumn() + "\"";
            String tcol = "\"" + targetTable + "\".\"" + RelationQueryBuilder.TableUtils.columnName(prop) + "\"";
            return "exists (select 1 from \"" + join + "\" join \"" + targetTable + "\" on " + tLeft + " = " + tRight + " where " + sLeft + " = " + sRight + " and " + tcol + " " + opToSql(op) + " ?)";
        }
        return null;
    }

    private String opToSql(String op) {
        return switch (op) {
            case "ne" -> "<>";
            case "gt" -> ">";
            case "ge" -> ">=";
            case "lt" -> "<";
            case "le" -> "<=";
            case "like" -> "like";
            case "notlike" -> "not like";
            default -> "=";
        };
    }

    
}
