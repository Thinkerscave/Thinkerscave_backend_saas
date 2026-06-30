package com.thinkerscave.access.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.access.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_code", columnList = "user_code"),
                @Index(name = "idx_user_org", columnList = "organization_id"),
                @Index(name = "idx_user_status", columnList = "status")
        }
)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Tenant scoping — resolved from JWT / TenantContext.
     */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * Internal unique code. Example: USR000001
     */
    @Column(name = "user_code", nullable = false, unique = true, length = 50)
    private String userCode;

    /**
     * Login username.
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Login email.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Login mobile.
     */
    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    /**
     * BCrypt encrypted password.
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // ─── Profile Fields ────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    // ─── Status & Security ────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Builder.Default
    @Column(name = "mobile_verified")
    private Boolean mobileVerified = false;

    @Builder.Default
    @Column(name = "first_time_login")
    private Boolean firstTimeLogin = true;

    @Builder.Default
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Builder.Default
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "lock_expiry_at")
    private LocalDateTime lockExpiryAt;

    // ─── Relationships ────────────────────────────────────────────────────

    /**
     * Managed via UserRole join entity for fine-grained control.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * User-level permission overrides.
     */
    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserPermission> userPermissions = new HashSet<>();

}