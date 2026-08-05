package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.security.entity.UserSession;
import com.thinkerscave.security.enums.SessionStatus;
import com.thinkerscave.security.repository.UserSessionRepository;
import com.thinkerscave.shared.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Resolves users that must always be looked up against the platform ("public") schema,
 * even when the ambient request is a tenant/institution login.
 *
 * <p>A single Hibernate session/connection is bound to one tenant schema for its entire
 * transaction (see {@link com.thinkerscave.config.TenantIdentifierResolver}) — flipping
 * {@link TenantContext} mid-transaction does NOT re-route already-open queries. This
 * service uses {@code REQUIRES_NEW} so the lookup runs in a genuinely separate
 * transaction/connection, resolved fresh against "public" at the moment it starts.
 *
 * <p>Also owns the write-side operations (lockout counters, session, login history) for
 * users resolved from public while the ambient transaction is bound to a tenant schema —
 * such a user is detached from the ambient session, so writes must happen here instead.
 */
@Service
@RequiredArgsConstructor
public class PublicSchemaUserLookupService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserSessionRepository sessionRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * Must be called with {@link TenantContext} already set to "public" by the caller.
     * REQUIRES_NEW opens the transaction (and acquires the Hibernate connection) at the
     * moment this proxied method is entered — setting the context inside the method body
     * would be too late, since the tenant identifier is resolved at transaction start.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<User> findActiveOwnerForOrganization(String usernameOrEmail, Long organizationId) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .filter(candidate -> hasActiveRole(candidate, RoleType.ORGANIZATION_OWNER)
                        && organizationRepository.existsActiveOwnedOrganization(candidate.getId(), organizationId))
                .map(this::initializeRoles);
    }

    /**
     * Looks up any user by username/email directly against the public schema, in a
     * brand-new transaction. Used by {@link UserDetailsServiceImpl} as a fallback for
     * credential verification when the account (SUPER_ADMIN / Organization Owner) is
     * not present in the ambient (tenant) schema.
     *
     * <p>Must also be called with {@link TenantContext} already set to "public" — see note above.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<User> findAnyInPublicSchema(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .map(this::initializeRoles);
    }

    /**
     * The returned entity outlives this REQUIRES_NEW transaction/session (it is used by
     * the caller after this method returns, at which point the session is already closed),
     * so lazy collections must be initialized here while the session is still open —
     * otherwise accessing user.getUserRoles() later throws LazyInitializationException.
     */
    private User initializeRoles(User user) {
        user.getUserRoles().forEach(ur -> ur.getRole().getRoleType());
        return user;
    }

    /** Must be called with TenantContext already set to "public" — see notes above. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulLogin(Long userId, String refreshToken, String deviceName) {
        User managedUser = userRepository.findById(userId).orElseThrow();
        managedUser.setFailedLoginAttempts(0);
        managedUser.setAccountLocked(false);
        managedUser.setLastLoginAt(LocalDateTime.now());
        userRepository.save(managedUser);

        sessionRepository.save(UserSession.builder()
                .user(managedUser)
                .refreshToken(refreshToken)
                .deviceName(deviceName)
                .ipAddress("")
                .loginAt(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build());

        loginHistoryRepository.save(LoginHistory.builder()
                .user(managedUser)
                .status(LoginStatus.SUCCESS)
                .loginTime(LocalDateTime.now())
                .build());
    }

    /** Must be called with TenantContext already set to "public" — see notes above. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLoginAndLockout(Long userId, String reason, int maxFailedAttempts) {
        User managedUser = userRepository.findById(userId).orElseThrow();
        loginHistoryRepository.save(LoginHistory.builder()
                .user(managedUser)
                .status(LoginStatus.FAILED)
                .loginTime(LocalDateTime.now())
                .failureReason(reason)
                .build());

        managedUser.setFailedLoginAttempts(managedUser.getFailedLoginAttempts() + 1);
        if (managedUser.getFailedLoginAttempts() != null && managedUser.getFailedLoginAttempts() >= maxFailedAttempts) {
            managedUser.setAccountLocked(true);
        }
        userRepository.save(managedUser);
    }

    private boolean hasActiveRole(User user, RoleType roleType) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getActive()) && ur.getRole().getRoleType() == roleType);
    }
}
