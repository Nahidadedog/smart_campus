package com.huadi.campus.selection;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消费者。auto-ack 模式：方法正常返回即确认；抛出异常且 default-requeue-rejected=false 时
 * 消息进入死信队列，不会无限重投。
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.selection.persist", havingValue = "rabbit")
public class RabbitSelectionListener {

    private final SelectionPersistHandler handler;

    @RabbitListener(queues = RabbitSelectionConfig.QUEUE)
    public void onMessage(SelectionCommand command) {
        handler.handle(command);
    }
}
