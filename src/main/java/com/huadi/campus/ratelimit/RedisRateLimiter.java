package com.huadi.campus.ratelimit;

import com.huadi.campus.selection.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 令牌桶。整个"补充令牌 → 判断 → 扣减 → 续期"在一个 Lua 脚本里完成，多实例共享同一个桶。
 * 失败策略选择 fail-open：Redis 挂了就放行。理由是限流只防刷，不承担正确性——
 * 名额超卖由 StockService 兜底，宁可多放一些请求也不要让所有用户因为限流器故障而无法选课。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> TOKEN_BUCKET = script();

    private final StringRedisTemplate redis;
    private final RedisKeys keys;

    private static DefaultRedisScript<Long> script() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/rate_limit_token_bucket.lua"));
        script.setResultType(Long.class);
        return script;
    }

    public boolean tryAcquire(String key, int capacity, double refillPerSecond) {
        try {
            Long allowed = redis.execute(TOKEN_BUCKET, List.of(keys.rateLimit(key)),
                    String.valueOf(capacity), String.valueOf(refillPerSecond), "1");
            return allowed != null && allowed == 1L;
        } catch (DataAccessException e) {
            log.warn("限流器不可用，本次放行 key={}", key, e);
            return true;
        }
    }
}
