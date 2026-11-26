package com.huadi.campus.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流（基于 Redis 令牌桶，按 key 隔离）。
 * key 是 SpEL 表达式，可引用方法参数，例如 "'select:' + #principal.id()" 表示每个用户一个桶。
 * capacity / refillPerSecond 留空（-1）时使用全局配置 app.selection.rate-limit.*，便于不改代码调参。
 * 仅当 app.selection.rate-limit.enabled=true 时生效（依赖 Redis）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key();

    /** 桶容量：瞬间最多允许的请求数；-1 表示用全局配置 */
    int capacity() default -1;

    /** 每秒补充的令牌数；-1 表示用全局配置 */
    double refillPerSecond() default -1;
}
