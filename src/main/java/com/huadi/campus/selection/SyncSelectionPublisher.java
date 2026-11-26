package com.huadi.campus.selection;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** 请求线程内直接落库：没有异步环节，响应返回时数据库里已经有记录。 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.selection.persist", havingValue = "sync", matchIfMissing = true)
public class SyncSelectionPublisher implements SelectionPersistPublisher {

    private final SelectionPersistHandler handler;

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void publish(SelectionCommand command) {
        try {
            handler.handle(command);
        } catch (RuntimeException e) {
            // 名额已在 handler 里回补，这里只转成对用户友好的错误
            throw new SelectionPersistException(e);
        }
    }
}
