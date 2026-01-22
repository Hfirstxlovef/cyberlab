package org.cyberlab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            logger.warn("Redis缓存设置失败，key: {}", key, e);
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.warn("Redis缓存获取失败，key: {}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            logger.warn("Redis缓存删除失败，key: {}", key, e);
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.warn("Redis缓存检查失败，key: {}", key, e);
            return false;
        }
    }

    public void setWithExpire(String key, Object value, long seconds) {
        set(key, value, seconds, TimeUnit.SECONDS);
    }

    public <T> T getWithType(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return type.cast(value);
        } catch (ClassCastException e) {
            logger.warn("Redis缓存类型转换失败，key: {}, 期望类型: {}", key, type.getName(), e);
            return null;
        }
    }
}
