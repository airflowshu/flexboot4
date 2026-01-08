package com.yunlbd.flexboot4.query;

import com.mybatisflex.core.query.QueryWrapper;
import com.yunlbd.flexboot4.dto.SearchDto;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DefaultQueryWrapperBuilder extends AbstractQueryWrapperBuilder {

    private static final DefaultQueryWrapperBuilder INSTANCE = new DefaultQueryWrapperBuilder();

    public static DefaultQueryWrapperBuilder get() {
        return INSTANCE;
    }

    @Override
    public QueryWrapper build(SearchDto dto, Class<?> entityClass) {
        QueryWrapper qw = QueryWrapper.create().from(entityClass);
        RelationQueryBuilder.RelationContext ctx = RelationQueryBuilder.prepare(entityClass, dto);
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
            if (isAnd) {
                qw.and((java.util.function.Consumer<QueryWrapper>) w -> applyChildren(w, ctx, it.getChildren(), it.getLogic()));
            } else {
                qw.or((java.util.function.Consumer<QueryWrapper>) w -> applyChildren(w, ctx, it.getChildren(), it.getLogic()));
            }
            return;
        }
        if (it.getField() == null || it.getOp() == null) {
            return;
        }
        String field = it.getField();
        String op = it.getOp().trim().toLowerCase(Locale.ROOT);
        Object val = ValueConverter.convert(ctx, field, it.getVal());
        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            String alias = parts[0];
            String prop = parts[1];
            String rel = normalizeRel(ctx, alias);
            String targetTable = ctx.pathToTable.get(rel);
            if (targetTable == null) return;
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
