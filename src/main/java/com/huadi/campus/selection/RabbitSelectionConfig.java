package com.huadi.campus.selection;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑（persist=rabbit 时装配）：
 * <pre>
 * 生产者 --(exchange: campus.selection.exchange, rk: selection.persist)--> campus.selection.queue --> 消费者
 *                                                                        | 处理失败且不重新入队
 *                                                                        v
 *                                               (dlx: campus.selection.dlx, rk: dead) --> campus.selection.dlq
 * </pre>
 * 死信队列的意义：落库持续失败（例如数据库宕机）时消息不会无限重投拖垮消费者，
 * 而是落到 DLQ 等待人工或定时任务处理；名额已在消费失败时回补到 Redis。
 * Spring Boot 自动装配的 RabbitAdmin 会在连接建立时声明上面所有 Queue / Exchange / Binding。
 */
@Configuration
@ConditionalOnProperty(name = "app.selection.persist", havingValue = "rabbit")
public class RabbitSelectionConfig {

    public static final String EXCHANGE = "campus.selection.exchange";
    public static final String ROUTING_KEY = "selection.persist";
    public static final String QUEUE = "campus.selection.queue";
    public static final String DLX = "campus.selection.dlx";
    public static final String DLQ = "campus.selection.dlq";
    public static final String DEAD_ROUTING_KEY = "dead";

    @Bean
    public DirectExchange selectionExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue selectionQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding selectionBinding() {
        return BindingBuilder.bind(selectionQueue()).to(selectionExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange selectionDlx() {
        return ExchangeBuilder.directExchange(DLX).durable(true).build();
    }

    @Bean
    public Queue selectionDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding selectionDlqBinding() {
        return BindingBuilder.bind(selectionDlq()).to(selectionDlx()).with(DEAD_ROUTING_KEY);
    }

    /** 消息体用 JSON 而不是 Java 序列化：管理界面里能直接看懂，也便于其它语言消费 */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
