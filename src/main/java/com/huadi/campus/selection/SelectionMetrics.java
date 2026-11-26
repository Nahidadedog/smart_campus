package com.huadi.campus.selection;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/** 选课链路的业务指标（/actuator/prometheus 暴露），用来在压测中区分成功 / 重复 / 售罄 / 降级 / 投递失败。 */
@Component
public class SelectionMetrics {

    private final Map<ReserveResult, Counter> reserveCounters = new EnumMap<>(ReserveResult.class);
    private final Counter degraded;
    private final Counter publishFailed;
    private final Counter persistFailed;

    public SelectionMetrics(MeterRegistry registry) {
        for (ReserveResult result : ReserveResult.values()) {
            reserveCounters.put(result, Counter.builder("campus.selection.requests")
                    .description("选课请求按结果计数")
                    .tag("outcome", result.name().toLowerCase())
                    .register(registry));
        }
        this.degraded = Counter.builder("campus.selection.degraded")
                .description("Redis 不可用降级为数据库扣减的次数")
                .register(registry);
        this.publishFailed = Counter.builder("campus.selection.publish.failed")
                .description("消息投递失败改为同步落库的次数")
                .register(registry);
        this.persistFailed = Counter.builder("campus.selection.persist.failed")
                .description("落库失败并回补名额的次数")
                .register(registry);
    }

    public void reserve(ReserveResult result) {
        reserveCounters.get(result).increment();
    }

    public void degraded() {
        degraded.increment();
    }

    public void publishFailed() {
        publishFailed.increment();
    }

    public void persistFailed() {
        persistFailed.increment();
    }
}
