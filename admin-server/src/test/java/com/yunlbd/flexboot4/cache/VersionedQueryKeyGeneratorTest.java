package com.yunlbd.flexboot4.cache;

import com.mybatisflex.core.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedQueryKeyGeneratorTest {
    @AfterEach
    void resetProvider() {
        TableVersions.setProvider(new TableVersionProvider() {
            @Override
            public long getVersion(String table) {
                return 0L;
            }

            @Override
            public long bumpVersion(String table) {
                return 0L;
            }
        });
    }

    @Test
    void keyChangesWhenTableVersionBumps() throws Exception {
        Map<String, Long> versions = new ConcurrentHashMap<>();
        TableVersions.setProvider(new TableVersionProvider() {
            @Override
            public long getVersion(String table) {
                return versions.getOrDefault(table, 0L);
            }

            @Override
            public long bumpVersion(String table) {
                return versions.merge(table, 1L, Long::sum);
            }
        });

        QueryWrapper qw = QueryWrapper.create().from("sys_user");

        VersionedQueryKeyGenerator kg = new VersionedQueryKeyGenerator();
        Method m = Dummy.class.getDeclaredMethod("list", QueryWrapper.class);

        String k1 = (String) kg.generate(new Dummy(), m, qw);
        TableVersions.bumpVersion("sys_user");
        String k2 = (String) kg.generate(new Dummy(), m, qw);

        assertNotEquals(k1, k2);
        assertTrue(k2.contains("sys_user="));
    }

    static class Dummy {
        @SuppressWarnings("unused")
        public void list(QueryWrapper queryWrapper) {
        }
    }
}
