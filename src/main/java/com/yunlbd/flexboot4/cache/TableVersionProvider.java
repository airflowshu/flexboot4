package com.yunlbd.flexboot4.cache;

public interface TableVersionProvider {
    long getVersion(String table);

    long bumpVersion(String table);
}

