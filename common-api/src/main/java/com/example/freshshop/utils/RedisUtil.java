package com.example.freshshop.utils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final StringRedisTemplate db0;
    private final StringRedisTemplate db1;
    private final StringRedisTemplate db2;

    // 🔥 强制 @Qualifier 精确绑定（这是你之前缺失的！！！）
    public RedisUtil(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisTemplateDb1") StringRedisTemplate redisTemplateDb1,
            @Qualifier("redisTemplateDb2") StringRedisTemplate redisTemplateDb2
    ) {
        this.db0 = stringRedisTemplate;
        this.db1 = redisTemplateDb1;
        this.db2 = redisTemplateDb2;
    }

    private StringRedisTemplate getDb(int db) {
        return switch (db) {
            case 1 -> db1;
            case 2 -> db2;
            default -> db0;
        };
    }

    public void set(int db, String key, String value) {
        getDb(db).opsForValue().set(key, value);
    }

    public void set(int db, String key, String value, long t, TimeUnit unit) {
        getDb(db).opsForValue().set(key, value, t, unit);
    }

    public String get(int db, String key) {
        return getDb(db).opsForValue().get(key);
    }

    public void delete(int db, String key) {
        getDb(db).delete(key);
    }

    public boolean hasKey(int db, String key) {
        return Boolean.TRUE.equals(getDb(db).hasKey(key));
    }

    public Long increment(int db, String key) {
        return getDb(db).opsForValue().increment(key);
    }

    public Long decrement(int db, String key) {
        return getDb(db).opsForValue().decrement(key);
    }
}