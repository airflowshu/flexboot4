package com.yunlbd.flexboot4.aigateway.util;

import java.util.List;

public final class PgVectorUtils {

    private PgVectorUtils() {
    }

    public static String toPgVectorString(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}
