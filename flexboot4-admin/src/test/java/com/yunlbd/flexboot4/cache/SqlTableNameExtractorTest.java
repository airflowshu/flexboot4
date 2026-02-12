package com.yunlbd.flexboot4.cache;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlTableNameExtractorTest {
    @Test
    void extractTablesFromJoinSql() {
        String sql = """
                select u.id, r.id
                from sys_user u
                left join sys_user_role ur on ur.user_id = u.id
                join public.sys_role r on r.id = ur.role_id
                where u.id = ?
                """;
        Set<String> tables = SqlTableNameExtractor.extractTables(sql);
        assertEquals(Set.of("sys_user", "sys_user_role", "sys_role"), tables);
    }
}

