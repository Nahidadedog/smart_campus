package com.huadi.campus.selection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ 生产者。两条失败路径：
 * <ol>
 *   <li>Broker 不可达：convertAndSend 同步抛出 AmqpException → 包装成 PublishUnavailableException，
 *       由 SelectionService 改为同步落库</li>
 *   <li>消息发出但没有收到 Broker 确认（publisher confirm 为 nack / 连接在确认前断开）：
 *       异步回调里回补名额，避免"名额扣了、记录没写"的漏单</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.selection.persist", havingValue = "rabbit")
public class RabbitSelectionPublisher implements SelectionPersistPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SelectionPersistHandler handler;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void publish(SelectionCommand command) {
        CorrelationData correlation = new CorrelationData(command.requestId());
        correlation.getFuture().whenComplete((confirm, ex) -> {
            if (ex != null || confirm == null || !confirm.isAck()) {
                log.error("消息未被 Broker 确认，回补名额 requestId={} cause={}",
                        command.requestId(), ex != null ? ex.getMessage() : confirm);
                handler.compensate(command);
            }
        });
        try {
            rabbitTemplate.convertAndSend(RabbitSelectionConfig.EXCHANGE, RabbitSelectionConfig.ROUTING_KEY,
                    command, correlation);
        } catch (AmqpException e) {
            throw new PublishUnavailableException(e);
        }
    }
}
