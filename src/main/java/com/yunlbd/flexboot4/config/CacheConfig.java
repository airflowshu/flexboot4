package com.yunlbd.flexboot4.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 使用 RedisSerializer.json() 获取通用的 JSON 序列化器 (推荐方式)
        RedisSerializer<Object> serializer = RedisSerializer.json();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL 30 minutes
                // 使用单冒号作为前缀分隔符，默认为双冒号 "::"
                .computePrefixWith(name -> name + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
                // .disableCachingNullValues(); // 允许缓存 null 值，避免 Cache 'xxx' does not allow 'null' values 错误

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    @Bean
    public DynamicCacheResolver dynamicCacheResolver(org.springframework.cache.CacheManager cacheManager) {
        return new DynamicCacheResolver(cacheManager);
    }
}
