package com.huadi.campus.selection;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.huadi.campus.config.SelectionProperties;
import com.huadi.campus.entity.Student;
import com.huadi.campus.exception.ResourceNotFoundException;
import com.huadi.campus.mapper.CourseMapper;
import com.huadi.campus.mapper.StudentMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 进程内热点缓存（Caffeine）：
 * <ul>
 *   <li>userId -> studentId：JWT 里只有 userId，选课要的是 student.id，不能每次都查库</li>
 *   <li>courseId -> 课程快照：存在性 + 名额上限 + 选课时间窗口</li>
 * </ul>
 * 这两层只是"减少无效流量"，真正的名额判断在 StockService，所以允许短暂不一致。
 * <p>
 * <b>为什么是 AsyncCache 而不是同步的 {@code cache.get(key, loader)}：</b><br>
 * 同步缓存的 loader 是在 ConcurrentHashMap 的桶锁（bin lock）里执行的，也就是说
 * "持桶锁等数据库连接"和"持数据库连接等桶锁"这两种顺序会同时出现——锁顺序反转，典型死锁。
 * 压测里它真实发生过：300 人同时选课，10 个连接的持有者卡在桶锁上，桶锁的持有者卡在
 * HikariPool.getConnection 上，直到 connectionTimeout（30s）把等待者打断才解开，
 * 表现就是几乎所有请求 60s 超时、数据库却一个事务都没有。
 * <p>
 * 现在的做法：
 * <ol>
 *   <li>桶锁里只做一件事——把一个 CompletableFuture 放进缓存；查库在独立的加载线程池里跑</li>
 *   <li>加载线程池大小 &lt; 连接池大小：每个加载任务占一个连接，要给请求线程自己的 UPDATE / INSERT 留余量</li>
 *   <li>如果在数据库事务里调用（当前线程已经占着一个事务连接），就用当前线程、当前连接加载，
 *       不能占着一个连接再去等加载线程借第二个连接</li>
 *   <li>加载线程排队满时退化为调用线程加载：此时线程不持有任何连接，只会慢，不会死锁</li>
 * </ol>
 * 同一个 key 并发未命中时 Caffeine 只会触发一次加载，其余线程 join 同一个 Future（缓存击穿防护保留）。
 */
@Slf4j
@Component
public class SelectionLocalCache {

    private final AsyncCache<Long, Long> studentIds;
    private final AsyncCache<Long, CourseStockView> courses;
    private final ThreadPoolExecutor loaderExecutor;

    public SelectionLocalCache(SelectionProperties properties,
                               @Value("${spring.datasource.hikari.maximum-pool-size:10}") int dbPoolSize) {
        SelectionProperties.LocalCache cfg = properties.getLocalCache();
        int threads = Math.max(1, Math.min(cfg.getLoaderThreads(), dbPoolSize / 2));
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory("cache-loader-");
        threadFactory.setDaemon(true);
        this.loaderExecutor = new ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1024), threadFactory);
        this.studentIds = Caffeine.newBuilder()
                .maximumSize(cfg.getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(cfg.getStudentTtlSeconds()))
                .buildAsync();
        this.courses = Caffeine.newBuilder()
                .maximumSize(cfg.getMaxSize())
                .expireAfterWrite(Duration.ofSeconds(cfg.getCourseTtlSeconds()))
                .buildAsync();
        log.info("选课本地缓存加载线程池：{} 个线程（数据库连接池 {}）", threads, dbPoolSize);
    }

    /** userId -> student.id，不是学生则 404（抛异常，不缓存，避免污染） */
    public Long studentIdOf(Long userId, StudentMapper studentMapper) {
        return load(studentIds, userId, () -> studentMapper.selectByUserId(userId)
                .map(Student::getId)
                .orElseThrow(() -> new ResourceNotFoundException("学生信息不存在")));
    }

    /** 课程快照；不存在的课程也以 MISSING 形式短暂缓存（防穿透），调用方拿到后转 404 */
    public CourseStockView courseOf(Long courseId, CourseMapper courseMapper) {
        return load(courses, courseId, () -> CourseStockView.of(courseMapper.selectById(courseId)));
    }

    /** 立刻失效课程快照；调用方需保证此时没有未提交的数据库事务，否则并发读会把旧值填回去 */
    public void evictCourse(Long courseId) {
        courses.asMap().remove(courseId);
    }

    /**
     * 课程被管理员修改后失效：若当前在事务里则等提交后再失效，
     * 否则提交前的并发读会把旧值重新填回缓存。
     */
    public void evictCourseAfterCommit(Long courseId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictCourse(courseId);
                }
            });
        } else {
            evictCourse(courseId);
        }
    }

    @PreDestroy
    void shutdown() {
        loaderExecutor.shutdown();
    }

    private <T> T load(AsyncCache<Long, T> cache, Long key, Supplier<T> loader) {
        CompletableFuture<T> future;
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            future = cache.get(key, (k, executor) -> CompletableFuture.completedFuture(loader.get()));
        } else {
            try {
                future = cache.get(key, (k, executor) -> CompletableFuture.supplyAsync(loader, loaderExecutor));
            } catch (RejectedExecutionException e) {
                log.warn("缓存加载线程池已满，退化为调用线程加载 key={}", key);
                future = cache.get(key, (k, executor) -> CompletableFuture.completedFuture(loader.get()));
            }
        }
        try {
            return future.join();
        } catch (CompletionException e) {
            // supplyAsync 会把 loader 抛出的业务异常包一层 CompletionException，这里还原成原来的异常
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw cause instanceof RuntimeException runtime ? runtime : e;
        }
    }
}
