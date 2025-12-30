package com.auth_service.auth_service.Aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Arrays;

/**
 * Aspect for logging method executions, exceptions, and performance metrics
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all public methods in controller classes
     */
    @Pointcut("execution(public * com..Controllers..*(..))")
    public void controllerMethods() {}

    /**
     * Pointcut for all public methods in service classes
     */
    @Pointcut("execution(public * com..Service..*(..))")
    public void serviceMethods() {}

    /**
     * Pointcut for all public methods in repository classes
     */
    @Pointcut("execution(public * com..Repository..*(..))")
    public void repositoryMethods() {}

    /**
     * Log before method execution
     */
    @Before("controllerMethods() || serviceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("→ Entering: {}.{}() with arguments: {}",
                className, methodName, Arrays.toString(args));
    }

    /**
     * Log after successful method execution
     */
    @AfterReturning(pointcut = "controllerMethods() || serviceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.info("← Exiting: {}.{}() with result: {}",
                className, methodName, result);
    }

    /**
     * Log after method throws exception
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.error("✗ Exception in: {}.{}() - Message: {}",
                className, methodName, exception.getMessage(), exception);
    }

    /**
     * Log method execution time (Around advice)
     */
    @Around("controllerMethods() || serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            stopWatch.stop();
            log.info("⏱ Execution time: {}.{}() took {} ms",
                    className, methodName, stopWatch.getTotalTimeMillis());
        }
    }
}