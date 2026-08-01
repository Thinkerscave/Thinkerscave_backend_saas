package com.thinkerscave.security.service.impl;

import com.thinkerscave.access.entity.LoginHistory;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.entity.UserRole;
import com.thinkerscave.access.enums.LoginStatus;
import com.thinkerscave.access.enums.RoleType;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.mapper.UserMapper;
import com.thinkerscave.access.entity.SecurityPolicy;
import com.thinkerscave.access.repository.SecurityPolicyRepository;
import com.thinkerscave.access.repository.LoginHistoryRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.security.dto.LoginContext;
import com.thinkerscave.security.dto.request.LoginRequest;
import com.thinkerscave.security.dto.response.AuthResponse;
import com.thinkerscave.security.dto.response.SessionResponse;
import com.thinkerscave.security.entity.UserSession;
import com.thinkerscave.security.enums.SessionStatus;
import com.thinkerscave.security.repository.UserSessionRepository;
import com.thinkerscave.security.service.AuthService;
import com.thinkerscave.security.service.JwtService;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final OrganizationRepository organizationRepository;
    private final UserSessionRepository sessionRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityPolicyRepository securityPolicyRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @org.springframework.beans.factory.annotation.Value("${app.security.lockout.max-failed-attempts:5}")
    private int defaultMaxFailedAttempts;

    @org.springframework.beans.factory.annotation.Value("${app.security.lockout.retry-delay-ms:500}")
    private long retryDelayMs;

    @org.springframework.beans.factory.annotation.Value("${jwt.expiration:900000}")
    private long accessTokenExpirationMs;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, LoginContext loginContext) {
        User user = resolveUserForLogin(request.getUsernameOrEmail(), loginContext);

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            recordLoginFailure(user, "Account locked");
            throw new BadRequestException("Account is locked due to too many failed login attempts. Contact an administrator.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            recordLoginFailure(user, "Invalid password");
            applyFailedLoginLockout(user, loginContext);
            // Uniform message + small delay frustrates brute force without leaking account state
            sleepBriefly();
            throw new BadRequestException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String tenantId = loginContext.isPlatformLogin()
                ? LoginContext.PLATFORM_TENANT
                : loginContext.getTenantIdentifier();
        String loginContextValue = loginContext.isPlatformLogin()
                ? LoginContext.PLATFORM
                : LoginContext.TENANT;

        Long effectiveOrgId = loginContext.isPlatformLogin() ? null : resolveTenantRegistry(loginContext).getOrganization().getId();
        Map<String, Object> claims = buildTokenClaims(user, tenantId, loginContextValue, effectiveOrgId);

        String accessToken = jwtService.generateAccessToken(user.getUsername(), claims);
        boolean rememberMe = Boolean.TRUE.equals(request.getRememberMe());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername(), rememberMe);

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceName(request.getDeviceName())
                .ipAddress("")
                .loginAt(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build();
        sessionRepository.save(session);

        recordLoginSuccess(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .tenantId(tenantId)
                .loginContext(loginContextValue)
                .user(userMapper.toSummary(user))
                .firstTimeLogin(user.getFirstTimeLogin())
                .requirePasswordChange(Boolean.TRUE.equals(user.getFirstTimeLogin()))
                .rememberMe(rememberMe)
                .build();
    }

    private User resolveUserForLogin(String usernameOrEmail, LoginContext loginContext) {
        if (loginContext.isPlatformLogin()) {
            User user = findByUsernameOrEmail(usernameOrEmail)
                    .orElseThrow(() -> new BadRequestException("Invalid credentials"));
            if (!hasActiveRole(user, RoleType.SUPER_ADMIN)) {
                throw new BadRequestException("This account is not authorized for Thinkers Department login");
            }
            return user;
        }

        TenantRegistry tenant = resolveTenantRegistry(loginContext);
        Long organizationId = tenant.getOrganization().getId();

        if (loginContext.getOrganizationId() != null && !organizationId.equals(loginContext.getOrganizationId())) {
            throw new BadRequestException("Organization does not match the selected institution");
        }

        User user = findByUsernameOrEmailAndOrganization(usernameOrEmail, organizationId)
            .orElseGet(() -> findByUsernameOrEmail(usernameOrEmail)
                .filter(candidate -> hasActiveRole(candidate, RoleType.ORGANIZATION_OWNER)
                    && organizationRepository.existsActiveOwnedOrganization(candidate.getId(), organizationId))
                .orElseThrow(() -> new BadRequestException("Invalid credentials for the selected institution")));

        if (hasActiveRole(user, RoleType.SUPER_ADMIN)) {
            throw new BadRequestException("Platform accounts must sign in through Thinkers Department");
        }

        return user;
    }

    private TenantRegistry resolveTenantRegistry(LoginContext loginContext) {
        if (loginContext.getTenantIdentifier() == null || loginContext.getTenantIdentifier().isBlank()) {
            throw new BadRequestException("Tenant is required for institution login");
        }

        // Tenant registry is authoritative on the platform catalog. Login requests
        // often arrive with X-Tenant-ID already set, which would otherwise switch
        // Hibernate to an empty/new tenant DB before this lookup runs.
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            return tenantRegistryRepository.findActiveByTenantIdentifierNormalized(loginContext.getTenantIdentifier())
                    .orElseThrow(() -> new BadRequestException("Unknown institution tenant"));
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }

    private Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));
    }

    private Optional<User> findByUsernameOrEmailAndOrganization(String usernameOrEmail, Long organizationId) {
        return userRepository.findByUsernameAndOrganizationId(usernameOrEmail, organizationId)
                .or(() -> userRepository.findByEmailAndOrganizationId(usernameOrEmail, organizationId));
    }

    private Map<String, Object> buildTokenClaims(User user, String tenantId, String loginContext, Long effectiveOrgId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("orgId", effectiveOrgId != null ? effectiveOrgId : user.getOrganizationId());
        claims.put("userCode", user.getUserCode());
        claims.put("tenant", tenantId);
        claims.put("loginContext", loginContext);

        UserRole primaryRole = user.getUserRoles().stream()
                .filter(ur -> Boolean.TRUE.equals(ur.getPrimaryRole()) && Boolean.TRUE.equals(ur.getActive()))
                .findFirst().orElse(null);
        if (primaryRole != null) {
            claims.put("roleType", primaryRole.getRole().getRoleType().name());
        }

        if (hasActiveRole(user, RoleType.ORGANIZATION_OWNER)) {
            List<Organization> ownedOrganizations = organizationRepository.findActiveByOwnerUserId(user.getId());
            List<String> switchableTenants = new ArrayList<>();
            List<Long> switchableOrgIds = new ArrayList<>();
            for (Organization org : ownedOrganizations) {
                if (org.getTenantRegistry() == null || org.getTenantRegistry().getTenantIdentifier() == null) {
                    continue;
                }
                switchableTenants.add(org.getTenantRegistry().getTenantIdentifier());
                switchableOrgIds.add(org.getId());
            }
            claims.put("switchableTenants", switchableTenants);
            claims.put("switchableOrgIds", switchableOrgIds);
            claims.put("tenantSwitchEnabled", true);
        }
        return claims;
    }

    private String resolveTenantForUser(User user) {
        if (hasActiveRole(user, RoleType.SUPER_ADMIN)) {
            return LoginContext.PLATFORM_TENANT;
        }
        if (hasActiveRole(user, RoleType.ORGANIZATION_OWNER)) {
            return organizationRepository.findActiveByOwnerUserId(user.getId()).stream()
                    .map(Organization::getTenantRegistry)
                    .filter(tr -> tr != null && tr.getTenantIdentifier() != null)
                    .map(TenantRegistry::getTenantIdentifier)
                    .findFirst()
                    .orElse(LoginContext.PLATFORM_TENANT);
        }
        return tenantRegistryRepository.findByOrganization_Id(user.getOrganizationId())
                .map(TenantRegistry::getTenantIdentifier)
                .orElse(LoginContext.PLATFORM_TENANT);
    }

    private boolean hasActiveRole(User user, RoleType roleType) {
        return user.getUserRoles().stream()
                .anyMatch(ur -> Boolean.TRUE.equals(ur.getActive())
                        && Boolean.TRUE.equals(ur.getPrimaryRole())
                        && ur.getRole().getRoleType() == roleType);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        UserSession session = sessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadRequestException("Session is no longer active");
        }

        User user = session.getUser();
        String tenantId = resolveTenantForUser(user);
        String loginContextValue = hasActiveRole(user, RoleType.SUPER_ADMIN)
                ? LoginContext.PLATFORM
                : LoginContext.TENANT;
        Map<String, Object> claims = buildTokenClaims(user, tenantId, loginContextValue, user.getOrganizationId());

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), claims);
        Boolean rememberMe = jwtService.extractRememberMe(refreshToken);
        boolean remember = Boolean.TRUE.equals(rememberMe);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername(), remember);

        session.setRefreshToken(newRefreshToken);
        sessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpirationMs / 1000)
                .tenantId(tenantId)
                .loginContext(loginContextValue)
                .user(userMapper.toSummary(user))
                .firstTimeLogin(user.getFirstTimeLogin())
                .requirePasswordChange(Boolean.TRUE.equals(user.getFirstTimeLogin()))
                .rememberMe(rememberMe)
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        sessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            session.setStatus(SessionStatus.LOGGED_OUT);
            session.setLogoutAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void logoutAllSessions(Long userId) {
        sessionRepository.terminateAllActiveSessions(userId, LocalDateTime.now());
        log.info("All sessions terminated for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SessionResponse> getUserSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUser_IdOrderByLoginAtDesc(userId, pageable)
                .map(this::mapSession);
    }

    @Override
    @Transactional
    public void terminateSession(Long sessionId) {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        session.setStatus(SessionStatus.LOGGED_OUT);
        session.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private void recordLoginSuccess(User user) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .status(LoginStatus.SUCCESS)
                .loginTime(LocalDateTime.now())
                .build();
        loginHistoryRepository.save(history);
    }

    private void recordLoginFailure(User user, String reason) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .status(LoginStatus.FAILED)
                .loginTime(LocalDateTime.now())
                .failureReason(reason)
                .build();
        loginHistoryRepository.save(history);
    }

    private void incrementFailedAttempts(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        userRepository.save(user);
    }

    private void applyFailedLoginLockout(User user, LoginContext loginContext) {
        incrementFailedAttempts(user);
        int maxAttempts = resolveMaxFailedAttempts(user, loginContext);
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= maxAttempts) {
            user.setAccountLocked(true);
            userRepository.save(user);
            log.warn("Account locked after {} failed login attempts (userId={})",
                    user.getFailedLoginAttempts(), user.getId());
        }
    }

    private int resolveMaxFailedAttempts(User user, LoginContext loginContext) {
        Long orgId = loginContext.getOrganizationId() != null
                ? loginContext.getOrganizationId()
                : user.getOrganizationId();
        if (orgId != null) {
            return securityPolicyRepository.findByOrganizationId(orgId)
                    .map(SecurityPolicy::getMaxFailedAttempts)
                    .filter(v -> v != null && v > 0)
                    .orElse(defaultMaxFailedAttempts);
        }
        return defaultMaxFailedAttempts;
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(Math.max(0, retryDelayMs));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private SessionResponse mapSession(UserSession s) {
        return SessionResponse.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .username(s.getUser().getUsername())
                .deviceName(s.getDeviceName())
                .browser(s.getBrowser())
                .operatingSystem(s.getOperatingSystem())
                .ipAddress(s.getIpAddress())
                .status(s.getStatus())
                .loginAt(s.getLoginAt())
                .logoutAt(s.getLogoutAt())
                .build();
    }
}
