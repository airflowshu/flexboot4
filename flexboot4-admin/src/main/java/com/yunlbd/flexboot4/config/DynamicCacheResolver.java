package com.yunlbd.flexboot4.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;

import java.util.Collection;
import java.util.Collections;

/**
 * 自定义 CacheResolver
 * 用于动态解析继承 BaseServiceImpl 的子类上的 @CacheConfig 注解中配置的 cacheNames
 */
public class DynamicCacheResolver extends SimpleCacheResolver {

    public DynamicCacheResolver(CacheManager cacheManager) {
        super(cacheManager);
    }

    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        // 1. 尝试获取方法或类上的 @Cacheable/@CacheEvict 等注解中显式指定的 cacheNames
        Collection<String> cacheNames = super.getCacheNames(context);
        if (!cacheNames.isEmpty()) {
            return cacheNames;
        }

        // 2. 如果注解中没有指定，则尝试获取目标类（子类）上的 @CacheConfig 注解
        Class<?> targetClass = context.getTarget().getClass();
        CacheConfig cacheConfig = targetClass.getAnnotation(CacheConfig.class);
        
        if (cacheConfig != null && cacheConfig.cacheNames().length > 0) {
            return java.util.Arrays.asList(cacheConfig.cacheNames());
        }
        
        // 3. 都没有配置，则无法解析，返回空列表（可能会导致异常）
        return Collections.emptyList();
    }
}
