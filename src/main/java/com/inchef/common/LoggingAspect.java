package com.inchef.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("within(com.inchef..*) && @within(org.springframework.web.bind.annotation.RestController)")
    public void controllerPointcut() {}

    @Pointcut("within(com.inchef..*) && @within(org.springframework.stereotype.Service)")
    public void servicePointcut() {}

    @Around("controllerPointcut()")
    public Object logControllerAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[REST REQUEST] -> {}.{}({})", className, methodName, Arrays.toString(args));
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[REST RESPONSE] <- {}.{}() completed in {} ms", className, methodName, elapsedTime);
            return result;
        } catch (Throwable ex) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[REST ERROR] !- {}.{}() failed in {} ms with exception: {}",
                    className, methodName, elapsedTime, ex.getMessage());
            throw ex;
        }
    }

    @Around("servicePointcut()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (log.isDebugEnabled()) {
            log.debug("[SERVICE START] {}.{}()", className, methodName);
        }
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
                log.debug("[SERVICE END] {}.{}() took {} ms", className, methodName, elapsedTime);
            }
            return result;
        } catch (Throwable ex) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.warn("[SERVICE EXCEPTION] {}.{}() threw {} in {} ms",
                    className, methodName, ex.getClass().getSimpleName(), elapsedTime);
            throw ex;
        }
    }
}
