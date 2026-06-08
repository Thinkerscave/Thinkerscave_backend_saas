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
                @Index(name = "idx_user_code", columnList = "user_code")
        }
)
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Internal unique code.
     * Example:
     * USR00001
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

    /**
     * Account status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Email verification.
     */
    @Builder.Default
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    /**
     * Mobile verification.
     */
    @Builder.Default
    @Column(name = "mobile_verified")
    private Boolean mobileVerified = false;

    /**
     * Force password reset on first login.
     */
    @Builder.Default
    @Column(name = "first_time_login")
    private Boolean firstTimeLogin = true;

    /**
     * Failed login attempts.
     */
    @Builder.Default
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    /**
     * Account lock flag.
     */
    @Builder.Default
    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    /**
     * Last login timestamp.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * Password change timestamp.
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * User roles.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

}