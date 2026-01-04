package com.tags_generation_service.tags_generation_service.Aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PerformanceMonitoringAspect {

    private final MeterRegistry meterRegistry;

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

            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "success")
                    .register(meterRegistry));

            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("method.execution.time")
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("status", "failure")
                    .register(meterRegistry));

            meterRegistry.counter("method.execution.errors",
                            "class", className,
                            "method", methodName,
                            "exception", e.getClass().getSimpleName())
                    .increment();

            throw e;
        }
    }
}