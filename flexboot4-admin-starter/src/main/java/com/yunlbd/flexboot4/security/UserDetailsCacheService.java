package com.yunlbd.flexboot4.security;

public interface UserDetailsCacheService {

    void evictUserCache(String username);
}
