package com.thinkerscave.shared.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around(
            "execution(* com.thinkerscave..controller..*(..)) || " +
            "execution(* com.thinkerscave..service..*(..))")
    public Object logMethodExecution(
            ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.currentTimeMillis();

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        String className =
                signature.getDeclaringType().getSimpleName();

        String methodName =
                signature.getName();

        String method =
                className + "." + methodName + "()";

        log.info("ENTER -> {}", method);

        try {

            Object result = joinPoint.proceed();

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.info(
                    "EXIT -> {} | {} ms",
                    method,
                    executionTime);

            return result;

        } catch (Exception ex) {

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.error(
                    "ERROR -> {} | {} ms | {}",
                    method,
                    executionTime,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }
}