package com.thinkerscave.common.auditing.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class for automatic audit logging.
 * 
 * When applied to a method, the AuditLogAspect will automatically:
 * 1. Log the method invocation
 * 2. Capture input parameters
 * 3. Record success/failure status
 * 4. Track execution duration
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * @Auditable(action = "CREATE_STUDENT", description = "Creates a new student record")
 * public Student createStudent(StudentDTO dto) {
 *     // method implementation
 * }
 * }
 * </pre>
 * 
 * @author System
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action name for audit logging.
     * Examples: "CREATE_STUDENT", "UPDATE_ORG", "DELETE_USER"
     */
    String action();

    /**
     * Human-readable description of the operation.
     */
    String description() default "";

    /**
     * Whether to log the method parameters.
     * Default: true
     */
    boolean logParams() default true;

    /**
     * Whether to log the return value.
     * Default: false (to avoid logging sensitive data)
     */
    boolean logResult() default false;

    /**
     * Specific parameter names to exclude from logging.
     * Use this to prevent logging sensitive fields like passwords.
     */
    String[] excludeParams() default { "password", "secret", "token", "credential" };
}
