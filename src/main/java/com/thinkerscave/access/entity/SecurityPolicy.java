package com.thinkerscave.access.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "security_policies",
        indexes = {
                @Index(name = "idx_security_policy_org", columnList = "organization_id", unique = true)
        }
)
public class SecurityPolicy extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * One policy per organization.
     */
    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    // ─── Password Complexity ───────────────────────────────────────────────

    @Builder.Default
    @Column(name = "min_password_length")
    private Integer minPasswordLength = 8;

    @Builder.Default
    @Column(name = "require_uppercase")
    private Boolean requireUppercase = true;

    @Builder.Default
    @Column(name = "require_lowercase")
    private Boolean requireLowercase = true;

    @Builder.Default
    @Column(name = "require_numbers")
    private Boolean requireNumbers = true;

    @Builder.Default
    @Column(name = "require_special_chars")
    private Boolean requireSpecialChars = false;

    @Builder.Default
    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays = 90;

    @Builder.Default
    @Column(name = "password_history_count")
    private Integer passwordHistoryCount = 5;

    // ─── Account Lockout ───────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "max_failed_attempts")
    private Integer maxFailedAttempts = 5;

    @Builder.Default
    @Column(name = "lockout_duration_minutes")
    private Integer lockoutDurationMinutes = 30;

    // ─── Session ──────────────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes = 60;

    @Builder.Default
    @Column(name = "max_concurrent_sessions")
    private Integer maxConcurrentSessions = 3;

    @Builder.Default
    @Column(name = "allow_remember_me")
    private Boolean allowRememberMe = false;

    // ─── MFA ──────────────────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "require_two_factor")
    private Boolean requireTwoFactor = false;

    // ─── Status ───────────────────────────────────────────────────────────

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
