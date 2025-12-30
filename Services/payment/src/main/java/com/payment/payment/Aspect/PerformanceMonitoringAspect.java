package com.payment.payment.Aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring performance metrics using Micrometer
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceMonitoringAspect {

    private final MeterRegistry meterRegistry;

    /**
     * Monitor method execution time and record metrics
     */
    @Around("@annotation(com.common.annotation.MonitorPerformance) || " +
            "execution(* com..Controllers..*(..)) || " +
            "execution(* com..Service..*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = String.format("%s.%s", className, methodName);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            // Record successful execution
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry));

            return result;
        } catch (Exception e) {
            // Record failed execution
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "failure")
                    .register(meterRegistry));

            // Increment error counter
            meterRegistry.counter("method.execution.errors",
                            "class", className,
                            "method", methodName,
                            "exception", e.getClass().getSimpleName())
                    .increment();

            throw e;
        }
    }
}