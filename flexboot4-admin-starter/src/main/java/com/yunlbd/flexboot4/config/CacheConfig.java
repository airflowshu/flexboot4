package com.yunlbd.flexboot4.config;

import com.yunlbd.flexboot4.cache.RedisTableVersionProvider;
import com.yunlbd.flexboot4.cache.TableVersionProvider;
import com.yunlbd.flexboot4.cache.TableVersions;
import com.yunlbd.flexboot4.cache.VersionedQueryKeyGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public org.springframework.cache.CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 使用 RedisSerializer.json() 获取通用的 JSON 序列化器 (推荐方式)
        RedisSerializer<Object> serializer = RedisSerializer.json();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL 30 minutes
                .computePrefixWith(name -> name + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    @Bean
    public DynamicCacheResolver dynamicCacheResolver(org.springframework.cache.CacheManager cacheManager) {
        return new DynamicCacheResolver(cacheManager);
    }

    @Bean
    public TableVersionProvider tableVersionProvider(ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider) {
        org.springframework.data.redis.core.StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            TableVersionProvider provider = new TableVersionProvider() {
                @Override
                public long getVersion(String table) {
                    return 0L;
                }

                @Override
                public long bumpVersion(String table) {
                    return 0L;
                }
            };
            TableVersions.setProvider(provider);
            return provider;
        }
        TableVersionProvider provider = new RedisTableVersionProvider(redis, "cache:ver:");
        TableVersions.setProvider(provider);
        return provider;
    }

    @Bean
    public org.springframework.cache.interceptor.KeyGenerator versionedQueryKeyGenerator() {
        return new VersionedQueryKeyGenerator();
    }
}
