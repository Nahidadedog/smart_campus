package com.huadi.campus.selection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动时预热 Redis 名额缓存（stock=redis 时生效）。
 * 放在 ApplicationRunner 里而不是 @PostConstruct：确保 Flyway 已执行、数据库可访问之后再读表。
 * 预热失败不阻塞启动——运行期首次访问会走懒加载。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.selection.stock", havingValue = "redis")
public class StockCacheWarmer implements ApplicationRunner {

    private final StockService stockService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            stockService.warmUp();
        } catch (RuntimeException e) {
            log.warn("Redis 名额缓存预热失败，将在首次访问时懒加载", e);
        }
    }
}
