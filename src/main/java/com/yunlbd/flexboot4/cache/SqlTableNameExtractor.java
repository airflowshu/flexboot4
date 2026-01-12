package com.yunlbd.flexboot4.cache;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlTableNameExtractor {
    private static final Pattern FROM_JOIN = Pattern.compile("(?i)\\b(from|join)\\s+([`\\\"\\[]?)([a-z0-9_\\.]+)\\2");

    private SqlTableNameExtractor() {
    }

    public static Set<String> extractTables(String sql) {
        if (sql == null || sql.isBlank()) {
            return Set.of();
        }
        String normalized = sql.replace('\n', ' ').replace('\r', ' ');
        Matcher matcher = FROM_JOIN.matcher(normalized);
        Set<String> tables = new LinkedHashSet<>();
        while (matcher.find()) {
            String name = matcher.group(3);
            if (name == null || name.isBlank()) {
                continue;
            }
            String t = name.toLowerCase(Locale.ROOT);
            int dot = t.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < t.length()) {
                t = t.substring(dot + 1);
            }
            tables.add(t);
        }
        return tables;
    }
}

