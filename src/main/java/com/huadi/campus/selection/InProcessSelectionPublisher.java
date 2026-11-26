package com.huadi.campus.selection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 进程内线程池异步落库：不依赖 MQ 也能演示"接口先返回、数据库稍后写入"。
 * 队列满时抛出 PublishUnavailableException，请求线程自己写库，形成背压。
 * 进程重启会丢失队列中的指令（名额在 Redis 里已扣但数据库没记录），需要刷新接口对账——这正是引入 MQ 的理由。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.selection.persist", havingValue = "in-process")
public class InProcessSelectionPublisher implements SelectionPersistPublisher {

    private final ThreadPoolTaskExecutor selectionExecutor;
    private final SelectionPersistHandler handler;

    public InProcessSelectionPublisher(@Qualifier("selectionExecutor") ThreadPoolTaskExecutor selectionExecutor,
                                       SelectionPersistHandler handler) {
        this.selectionExecutor = selectionExecutor;
        this.handler = handler;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void publish(SelectionCommand command) {
        try {
            selectionExecutor.execute(() -> safeHandle(command));
        } catch (TaskRejectedException e) {
            throw new PublishUnavailableException(e);
        }
    }

    private void safeHandle(SelectionCommand command) {
        try {
            handler.handle(command);
        } catch (RuntimeException e) {
            // handler 已回补名额，这里只记录
            log.error("线程池异步落库失败 requestId={}", command.requestId(), e);
        }
    }
}
