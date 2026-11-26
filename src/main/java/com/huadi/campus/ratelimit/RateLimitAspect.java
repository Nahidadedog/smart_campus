package com.huadi.campus.ratelimit;

import com.huadi.campus.config.SelectionProperties;
import com.huadi.campus.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 解析 @RateLimit 的 SpEL key 并调用限流器；超限抛 429，由全局异常处理器转成统一响应。
 * 注解里没写的参数（-1）回落到 app.selection.rate-limit.* 全局配置。
 */
@Aspect
@Component
@ConditionalOnProperty(name = "app.selection.rate-limit.enabled", havingValue = "true")
public class RateLimitAspect {

    private final RedisRateLimiter limiter;
    private final SelectionProperties properties;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();

    public RateLimitAspect(RedisRateLimiter limiter, SelectionProperties properties) {
        this.limiter = limiter;
        this.properties = properties;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext context = new MethodBasedEvaluationContext(
                null, method, joinPoint.getArgs(), parameterNames);
        String key = parser.parseExpression(rateLimit.key()).getValue(context, String.class);

        SelectionProperties.RateLimit global = properties.getRateLimit();
        int capacity = rateLimit.capacity() > 0 ? rateLimit.capacity() : global.getCapacity();
        double refill = rateLimit.refillPerSecond() > 0 ? rateLimit.refillPerSecond() : global.getRefillPerSecond();

        if (!limiter.tryAcquire(key, capacity, refill)) {
            throw new TooManyRequestsException("操作过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }
}
