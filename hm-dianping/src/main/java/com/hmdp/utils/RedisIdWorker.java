package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1727625661L;
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC); //将 LocalDateTime 对象转换为自1970年1月1日00:00:00 UTC（协调世界时）至该日期时间的秒数
        //ZoneOffset.UTC 表示的是协调世界时（UTC），没有时区偏差。

        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1 获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        //3.拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
