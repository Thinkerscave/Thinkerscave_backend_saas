package com.thinkerscave.common.orgm.validation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Comprehensive validator for organization-related operations.
 * Centralizes all validation logic for tenant/organization management.
 * 
 * @author System
 */
@Component
public class OrganizationValidator {

    // Reserved names that cannot be used as tenant/organization identifiers
    private static final List<String> RESERVED_NAMES = Arrays.asList(
            "public", "admin", "system", "api", "www", "app", "dashboard",
            "pg_catalog", "information_schema", "pg_toast", "master", "main",
            "default", "root", "super", "test", "demo", "staging", "production");

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern TENANT_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{2,30}$");

    private static final Pattern ORG_CODE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{2,30}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }

    /**
     * Validate tenant name for onboarding.
     */
    public ValidationResult validateTenantName(String tenantName) {
        List<String> errors = new ArrayList<>();

        if (tenantName == null || tenantName.isBlank()) {
            errors.add("Tenant name is required");
            return ValidationResult.failure(errors);
        }

        String sanitized = sanitizeTenantName(tenantName);

        if (!TENANT_NAME_PATTERN.matcher(sanitized).matches()) {
            errors.add("Tenant name must be 3-31 lowercase alphanumeric characters, starting with a letter");
        }

        if (RESERVED_NAMES.contains(sanitized.toLowerCase())) {
            errors.add("Tenant name '" + sanitized + "' is reserved and cannot be used");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate organization code.
     */
    public ValidationResult validateOrgCode(String orgCode) {
        List<String> errors = new ArrayList<>();

        if (orgCode == null || orgCode.isBlank()) {
            errors.add("Organization code is required");
            return ValidationResult.failure(errors);
        }

        if (!ORG_CODE_PATTERN.matcher(orgCode).matches()) {
            errors.add("Organization code must be 3-31 alphanumeric characters starting with a letter");
        }

        if (RESERVED_NAMES.contains(orgCode.toLowerCase())) {
            errors.add("Organization code '" + orgCode + "' is reserved");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate email address.
     */
    public ValidationResult validateEmail(String email) {
        List<String> errors = new ArrayList<>();

        if (email == null || email.isBlank()) {
            errors.add("Email is required");
            return ValidationResult.failure(errors);
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("Invalid email format: " + email);
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate password strength.
     */
    public ValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isBlank()) {
            errors.add("Password is required");
            return ValidationResult.failure(errors);
        }

        if (password.length() < 8) {
            errors.add("Password must be at least 8 characters");
        }

        if (password.length() > 72) {
            errors.add("Password must not exceed 72 characters");
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*[0-9].*")) {
            errors.add("Password must contain at least one digit");
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate phone number.
     */
    public ValidationResult validatePhone(String phone) {
        List<String> errors = new ArrayList<>();

        if (phone != null && !phone.isBlank()) {
            String digitsOnly = phone.replaceAll("[^0-9]", "");
            if (!PHONE_PATTERN.matcher(digitsOnly).matches()) {
                errors.add("Phone number must be 10-15 digits");
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate organization type.
     */
    public ValidationResult validateOrgType(String orgType) {
        List<String> errors = new ArrayList<>();
        List<String> validTypes = Arrays.asList(
                "COLLEGE", "SCHOOL", "UNIVERSITY", "INSTITUTE", "ACADEMY", "OTHER");

        if (orgType == null || orgType.isBlank()) {
            errors.add("Organization type is required");
            return ValidationResult.failure(errors);
        }

        if (!validTypes.contains(orgType.toUpperCase())) {
            errors.add("Invalid organization type. Valid types: " + String.join(", ", validTypes));
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Validate required string field.
     */
    public ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return ValidationResult.failure(List.of(fieldName + " is required"));
        }
        return ValidationResult.success();
    }

    /**
     * Validate string length.
     */
    public ValidationResult validateLength(String value, String fieldName, int min, int max) {
        List<String> errors = new ArrayList<>();

        if (value != null) {
            if (value.length() < min) {
                errors.add(fieldName + " must be at least " + min + " characters");
            }
            if (value.length() > max) {
                errors.add(fieldName + " must not exceed " + max + " characters");
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    /**
     * Sanitize tenant name for schema creation.
     */
    public String sanitizeTenantName(String tenantName) {
        if (tenantName == null)
            return null;
        return tenantName.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Check if a name is reserved.
     */
    public boolean isReservedName(String name) {
        return name != null && RESERVED_NAMES.contains(name.toLowerCase());
    }
}
