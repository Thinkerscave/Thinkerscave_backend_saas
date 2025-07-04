package com.thinkerscave.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * LoggingAspect logs method entry, exit, arguments, return values,
 * exceptions, and execution time for methods in controller and service layers.
 */
@Aspect // Marks this class as an Aspect (AOP component)
@Component // Marks this as a Spring-managed bean
public class LoggingAspect {

    // Logger for logging messages
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * This advice logs everything around controller and service methods:
     * - Input arguments
     * - Output result
     * - Execution time
     * - Exceptions (if any)
     *
     * The pointcut expression targets all methods inside:
     * - com.thinkerscave.common.orgm.controller..*
     * - com.thinkerscave.common.orgm.service..*
     */
    @Around("execution(* com.thinkerscave.common.orgm.controller..*(..)) || " +
            "execution(* com.thinkerscave.common.orgm.service..*(..)) || " +
            "execution(* com.thinkerscave.common.usrm.controller..*(..)) || " +
            "execution(* com.thinkerscave.common.usrm.service..*(..)) || " +
            "execution(* com.thinkerscave.common.role.controller..*(..)) || " +
            "execution(* com.thinkerscave.common.role.service..*(..))")
    public Object logAllMethodCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        // Record the start time of the method
        long startTime = System.currentTimeMillis();

        // Get method signature info (class + method name)
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        String fullMethod = className + "." + methodName + "()";

        // Get method arguments
        Object[] args = joinPoint.getArgs();
        logger.info("➡️ Entering: {} with arguments: {}", fullMethod, Arrays.toString(args));

        try {
            // Proceed with the actual method call
            Object result = joinPoint.proceed();

            // Calculate duration after execution
            long duration = System.currentTimeMillis() - startTime;

            // Log method exit and result
            logger.info("✅ Exiting: {} with result: {}", fullMethod, result);
            logger.info("⏱️ Execution time: {} ms", duration);

            return result;

        } catch (Throwable ex) {
            // Log exception details
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ Exception in: {}", fullMethod);
            logger.error("   Message: {}", ex.getMessage(), ex);
            logger.info("⏱️ Execution time before exception: {} ms", duration);

            // Rethrow exception so normal flow is not interrupted
            throw ex;
        }
    }
}
