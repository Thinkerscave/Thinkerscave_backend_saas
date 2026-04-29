package com.thinkerscave.common.orgm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Request DTO for tenant onboarding.
 * Contains all necessary information to provision a new tenant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantOnboardingRequest {

    @NotBlank(message = "Tenant name is required")
    @Pattern(regexp = "^[a-z0-9_]{3,50}$", message = "Tenant name must be 3-50 characters, lowercase, alphanumeric and underscores only")
    private String tenantName;

    @NotBlank(message = "Display name is required")
    @Size(min = 3, max = 500, message = "Display name must be between 3 and 500 characters")
    private String displayName;

    @Email(message = "Invalid admin email format")
    @NotBlank(message = "Admin email is required")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_])(?=\\S+$).{8,}$",
             message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and be at least 8 characters long.")
    private String adminPassword;

    private String adminFirstName = "Admin";

    private String adminLastName = "User";

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid mobile number format")
    private String adminMobile;

    private Boolean enableSubdomain = true;

    @Pattern(regexp = "^[a-z0-9-]{3,50}$", message = "Subdomain must be 3-50 characters, lowercase, alphanumeric and hyphens only")
    private String subdomainPrefix;

    @Pattern(regexp = "^(?i)(COLLEGE|SCHOOL|UNIVERSITY|INSTITUTE|ACADEMY|OTHER)$", message = "Invalid organization type. Valid types: COLLEGE, SCHOOL, UNIVERSITY, INSTITUTE, ACADEMY, OTHER")
    private String organizationType; // SCHOOL, COLLEGE, UNIVERSITY, INSTITUTE

    private String subscriptionType = "Free"; // Free, Paid, Premium

    private Integer maxUsers = 100;

    private Integer storageLimitMb = 10240; // 10GB default

    private Map<String, Object> customSettings;

    private String city;
    private String state;
    private java.time.LocalDate establishDate;

    // Audit fields
    @JsonIgnore
    private String ipAddress;

    @JsonIgnore
    private String performedBy;
}
