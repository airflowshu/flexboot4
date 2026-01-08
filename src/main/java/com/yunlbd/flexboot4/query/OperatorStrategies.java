package com.yunlbd.flexboot4.query;

import com.mybatisflex.core.query.QueryWrapper;

import java.util.Collection;

public class OperatorStrategies {
    public static void applyInto(QueryWrapper w, String col, String op, Object val) {
        switch (op) {
            case "eq" -> { if (val != null) w.eq(col, val); }
            case "ne" -> { if (val != null) w.ne(col, val); }
            case "gt" -> { if (val != null) w.gt(col, val); }
            case "ge" -> { if (val != null) w.ge(col, val); }
            case "lt" -> { if (val != null) w.lt(col, val); }
            case "le" -> { if (val != null) w.le(col, val); }
            case "like" -> { if (val != null) w.like(col, val); }
            case "notlike" -> { if (val != null) w.notLike(col, val); }
            case "in" -> {
                Collection<?> c = ValueConverter.toCollection(val);
                if (c != null && !c.isEmpty()) w.in(col, c);
            }
            case "notin" -> {
                Collection<?> c = ValueConverter.toCollection(val);
                if (c != null && !c.isEmpty()) w.notIn(col, c);
            }
            case "isnull" -> w.isNull(col);
            case "notnull" -> w.isNotNull(col);
            default -> {}
        }
    }
}
