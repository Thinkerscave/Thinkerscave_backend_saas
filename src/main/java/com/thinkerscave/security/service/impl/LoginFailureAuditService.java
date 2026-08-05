package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records failed-login audit history and lockout-counter updates for tenant-schema users
 * (i.e. everyone except the cross-schema ORGANIZATION_OWNER case handled by
 * {@link PublicSchemaUserLookupService}).
 *
 * <p>{@link AuthServiceImpl#login} is itself {@code @Transactional} and, on invalid
 * credentials, ultimately throws a {@link com.thinkerscave.shared.exceptions.BadRequestException}
 * to the caller. An unchecked exception escaping a Spring-managed transaction marks it
 * rollback-only, which would silently discard any {@code failed_login_attempts} increment
 * or {@link LoginHistory} row written earlier in the same method — making account lockout
 * and failed-login auditing complete no-ops. Using {@code REQUIRES_NEW} here commits these
 * writes in their own transaction before the outer transaction rolls back.
 */
@Service
@RequiredArgsConstructor
public class LoginFailureAuditService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(Long userId, String reason) {
        User managedUser = userRepository.findById(userId).orElseThrow();
        loginHistoryRepository.save(LoginHistory.builder()
                .user(managedUser)
                .status(LoginStatus.FAILED)
                .loginTime(java.time.LocalDateTime.now())
                .failureReason(reason)
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyFailedLoginLockout(Long userId, int maxFailedAttempts) {
        User managedUser = userRepository.findById(userId).orElseThrow();
        managedUser.setFailedLoginAttempts(managedUser.getFailedLoginAttempts() + 1);
        if (managedUser.getFailedLoginAttempts() != null && managedUser.getFailedLoginAttempts() >= maxFailedAttempts) {
            managedUser.setAccountLocked(true);
            managedUser.setLockedAt(java.time.LocalDateTime.now());
            managedUser.setStatus(com.thinkerscave.access.enums.UserStatus.LOCKED);
        }
        userRepository.save(managedUser);
    }
}
