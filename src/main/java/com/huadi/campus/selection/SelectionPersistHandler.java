package com.huadi.campus.selection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * 消费侧统一入口：落库 → 处理重复 → 失败时回补名额。
 * 同步模式、线程池模式、RabbitMQ 监听器都只调用这一个类，保证三种模式的失败语义完全一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionPersistHandler {

    /** 死锁 / 锁等待超时属于瞬时错误，重试几次通常就能成功，不应该直接回补名额进死信 */
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MILLIS = 50;

    private final SelectionPersistService persistService;
    private final StockService stockService;
    private final SelectionMetrics metrics;

    /**
     * 处理一条指令。落库失败时先回补名额，再把异常抛出：
     * - MQ 模式下监听容器会把消息转入死信队列，而不是无限重投；
     * - 同步 / 线程池模式下由调用方记录日志。
     */
    public void handle(SelectionCommand command) {
        PersistOutcome outcome;
        try {
            outcome = persistWithRetry(command);
        } catch (DuplicateKeyException e) {
            // 两个请求同时通过"是否已选"检查，数据库唯一约束兜底；先到的那条已经成功
            log.info("并发重复落库，由唯一约束拦截 requestId={}", command.requestId());
            outcome = PersistOutcome.DUPLICATE;
        } catch (RuntimeException e) {
            metrics.persistFailed();
            log.error("选课落库失败，回补名额 requestId={}", command.requestId(), e);
            compensate(command);
            throw e;
        }
        if (outcome == PersistOutcome.DUPLICATE) {
            log.info("落库时发现已选，归还预留名额 requestId={}", command.requestId());
            stockService.releaseReservation(command);
        }
    }

    /** 只回补名额，不碰数据库（消息没有到达 Broker 时使用） */
    public void compensate(SelectionCommand command) {
        try {
            stockService.releaseReservation(command);
        } catch (RuntimeException e) {
            log.error("名额回补失败，需要调用 /api/admin/cache/courses/refresh 对账 requestId={}", command.requestId(), e);
        }
    }

    private PersistOutcome persistWithRetry(SelectionCommand command) {
        for (int attempt = 1; ; attempt++) {
            try {
                // persist 是 @Transactional，抛出时事务已经回滚，重试会开一个新事务
                return persistService.persist(command);
            } catch (PessimisticLockingFailureException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("落库遇到锁冲突，第 {} 次重试 requestId={} cause={}",
                        attempt, command.requestId(), e.getMostSpecificCause().getMessage());
                try {
                    Thread.sleep(RETRY_BACKOFF_MILLIS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
