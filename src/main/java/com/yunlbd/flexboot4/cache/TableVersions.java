package com.yunlbd.flexboot4.cache;

import java.util.Objects;

public final class TableVersions {
    private static volatile TableVersionProvider provider = new TableVersionProvider() {
        @Override
        public long getVersion(String table) {
            return 0L;
        }

        @Override
        public long bumpVersion(String table) {
            return 0L;
        }
    };

    private TableVersions() {
    }

    public static long getVersion(String table) {
        return provider.getVersion(table);
    }

    public static long bumpVersion(String table) {
        return provider.bumpVersion(table);
    }

    public static void setProvider(TableVersionProvider p) {
        if (p != null) {
            provider = Objects.requireNonNull(p);
        }
    }
}

