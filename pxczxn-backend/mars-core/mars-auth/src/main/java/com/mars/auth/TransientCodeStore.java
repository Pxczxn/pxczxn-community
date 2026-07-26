package com.mars.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码等短期数据存储。
 *
 * <p>优先使用 Redis；Redis 不可用时自动退化到当前进程内存，
 * 保证单机开发环境仍能完成后台登录。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransientCodeStore {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalEntry> localEntries = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable = true;

    public void put(String key, String value, Duration ttl) {
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                return;
            } catch (RuntimeException e) {
                switchToLocalStore();
            }
        }
        localEntries.put(key, new LocalEntry(value, Instant.now().plus(ttl)));
    }

    public String get(String key) {
        if (redisAvailable) {
            try {
                return redisTemplate.opsForValue().get(key);
            } catch (RuntimeException e) {
                switchToLocalStore();
            }
        }
        LocalEntry entry = localEntries.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            localEntries.remove(key);
            return null;
        }
        return entry.value();
    }

    public boolean hasKey(String key) {
        return get(key) != null;
    }

    public void delete(String key) {
        localEntries.remove(key);
        if (redisAvailable) {
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException e) {
                switchToLocalStore();
            }
        }
    }

    private void switchToLocalStore() {
        if (redisAvailable) {
            redisAvailable = false;
            log.warn("Redis 不可用，验证码存储降级为进程内缓存");
        }
    }

    private record LocalEntry(String value, Instant expiresAt) {
    }
}
