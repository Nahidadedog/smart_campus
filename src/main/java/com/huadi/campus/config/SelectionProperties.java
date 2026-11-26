package com.huadi.campus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 高并发选课相关开关。
 * <pre>
 * app.selection.stock   = db | redis        名额在哪一层预占：数据库原子 UPDATE / Redis Lua 脚本
 * app.selection.persist = sync | in-process | rabbit   选课记录如何落库：同步 / 进程内线程池 / RabbitMQ 消费者
 * app.selection.rate-limit.enabled          接口限流（依赖 Redis，Redis 不可用时自动放行）
 * </pre>
 * 两种 stock 模式可以和三种 persist 模式自由组合，便于对比压测。
 */
@Data
@ConfigurationProperties(prefix = "app.selection")
public class SelectionProperties {

    private StockMode stock = StockMode.DB;

    private PersistMode persist = PersistMode.SYNC;

    /** Redis key 前缀，多个环境共用一个 Redis 时用来隔离 */
    private String redisKeyPrefix = "campus";

    private final RateLimit rateLimit = new RateLimit();

    private final LocalCache localCache = new LocalCache();

    public enum StockMode {
        /** 纯数据库：依赖 course.current_students 的原子 UPDATE */
        DB,
        /** Redis：Lua 脚本原子预扣，数据库只做最终落库 */
        REDIS
    }

    public enum PersistMode {
        /** 请求线程内同步写库 */
        SYNC,
        /** 进程内线程池异步写库（演示用，不依赖 MQ） */
        IN_PROCESS,
        /** RabbitMQ：生产者投递，消费者落库 */
        RABBIT
    }

    @Data
    public static class RateLimit {
        private boolean enabled = false;
        /** 桶容量：瞬间允许的请求数 */
        private int capacity = 5;
        /** 每秒补充的令牌数 */
        private double refillPerSecond = 1.0;
    }

    @Data
    public static class LocalCache {
        /** 课程名额快照在本地缓存的秒数（窗口校验用，不参与扣减） */
        private long courseTtlSeconds = 10;
        /** userId -> studentId 映射缓存秒数 */
        private long studentTtlSeconds = 300;
        private int maxSize = 4096;
        /**
         * 缓存未命中时查库的线程数。每个加载任务占一个数据库连接，
         * 实际线程数会被限制在 连接池大小 / 2 以内，给请求线程自己的写库留余量
         */
        private int loaderThreads = 4;
    }
}
