package com.huadi.campus.config;

import com.huadi.campus.selection.DbStockService;
import com.huadi.campus.selection.RedisKeys;
import com.huadi.campus.selection.RedisStockService;
import com.huadi.campus.selection.StockService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 高并发选课的装配：
 * <ul>
 *   <li>按 app.selection.stock 选择主 StockService；DbStockService 始终存在，Redis 不可用时降级用</li>
 *   <li>in-process 模式的落库线程池：有界队列 + AbortPolicy，队列满时抛出异常让请求线程自己写库（背压）</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(SelectionProperties.class)
public class SelectionConfig {

    @Bean
    public RedisKeys redisKeys(SelectionProperties properties) {
        return new RedisKeys(properties.getRedisKeyPrefix());
    }

    @Bean
    @Primary
    public StockService stockService(SelectionProperties properties,
                                     DbStockService dbStockService,
                                     ObjectProvider<RedisStockService> redisStockService) {
        if (properties.getStock() == SelectionProperties.StockMode.REDIS) {
            return redisStockService.getObject();
        }
        return dbStockService;
    }

    @Bean(name = "selectionExecutor")
    @ConditionalOnProperty(name = "app.selection.persist", havingValue = "in-process")
    public ThreadPoolTaskExecutor selectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("selection-persist-");
        // 队列满直接拒绝，由调用方降级为同步落库；相当于 CallerRunsPolicy，但能被指标观测到
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
