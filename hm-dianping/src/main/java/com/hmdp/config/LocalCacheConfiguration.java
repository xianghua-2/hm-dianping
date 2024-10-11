package com.hmdp.config;


import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存Caffeine配置类
 */
@Configuration
public class LocalCacheConfiguration {

    @Bean("localCacheManager")
    public Cache<String, Object> localCacheManager() {
        return Caffeine.newBuilder()
                //写入或者更新5s后，缓存过期并失效, 实际项目中肯定不会那么短时间就过期，根据具体情况设置即可
                .expireAfterWrite(120, TimeUnit.SECONDS)
                // 初始的缓存空间大小
                .initialCapacity(50)
                // 缓存的最大条数，通过 Window TinyLfu算法控制整个缓存大小
                .maximumSize(500)
                //打开数据收集功能
                .recordStats()
                .build();
    }

}