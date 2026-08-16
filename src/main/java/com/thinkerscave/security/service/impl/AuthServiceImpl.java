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
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.OrganizationStatus;
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
    private final PublicSchemaUserLookupService publicSchemaUserLookupService;
    private final LoginFailureAuditService loginFailureAuditService;

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
        // ORGANIZATION_OWNER accounts always live in "public"; if this is a tenant/institution
        // login, the ambient transaction is bound to that tenant's schema, so `user` is
        // detached from it — all writes for this user must run in their own "public"
        // transaction instead (see PublicSchemaUserLookupService).
        boolean crossSchemaUser = !loginContext.isPlatformLogin() && hasActiveRole(user, RoleType.ORGANIZATION_OWNER);

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            if (!crossSchemaUser) {
                loginFailureAuditService.recordLoginFailure(user.getId(), "Account locked");
            }
            throw new BadRequestException("Account is locked due to too many failed login attempts. Contact an administrator.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            if (crossSchemaUser) {
                runInPublicSchema(() -> publicSchemaUserLookupService.recordFailedLoginAndLockout(
                        user.getId(), "Invalid password", resolveMaxFailedAttempts(user, loginContext)));
            } else {
                loginFailureAuditService.recordLoginFailure(user.getId(), "Invalid password");
                loginFailureAuditService.applyFailedLoginLockout(user.getId(), resolveMaxFailedAttempts(user, loginContext));
            }
            // Uniform message + small delay frustrates brute force without leaking account state
            sleepBriefly();
            throw new BadRequestException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("Account is not active");
        }

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

        if (crossSchemaUser) {
            runInPublicSchema(() -> publicSchemaUserLookupService.recordSuccessfulLogin(
                    user.getId(), refreshToken, request.getDeviceName()));
        } else {
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

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
        }

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
        Organization organization = tenant.getOrganization();
        Long organizationId = organization.getId();

        if (loginContext.getOrganizationId() != null && !organizationId.equals(loginContext.getOrganizationId())) {
            throw new BadRequestException("Organization does not match the selected institution");
        }

        if (organization.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new BadRequestException("This organization has been suspended. Please contact support for assistance.");
        }

        if (organization.getCustomer() != null 
                && organization.getCustomer().getStatus() == CustomerStatus.SUSPENDED) {
            throw new BadRequestException("Account access has been suspended. Please contact support for assistance.");
        }

        // 1) Exact org match (platform catalog / correctly provisioned rows)
        // 2) Tenant-schema users may have organization_id unset/0 — schema isolation already scopes them
        // 3) ORGANIZATION_OWNER accounts live only in the public schema (they can own/switch
        //    across multiple organizations). A single Hibernate session/transaction is bound
        //    to one schema for its whole lifetime (see TenantIdentifierResolver) — flipping
        //    TenantContext here would NOT re-route the ambient (tenant-schema) queries above,
        //    so this lookup must run in a genuinely separate transaction/connection. The
        //    tenant identifier for that new transaction is resolved the instant it is opened
        //    (at proxy entry), so TenantContext must be switched to "public" BEFORE calling in.
        User user = findByUsernameOrEmailAndOrganization(usernameOrEmail, organizationId)
            .or(() -> findByUsernameOrEmail(usernameOrEmail)
                .filter(candidate -> {
                    Long candidateOrgId = candidate.getOrganizationId();
                    return candidateOrgId == null || candidateOrgId <= 0 || organizationId.equals(candidateOrgId);
                }))
            .or(() -> lookupOwnerInPublicSchema(usernameOrEmail, organizationId))
            .orElseThrow(() -> new BadRequestException("Invalid credentials for the selected institution"));

        if (hasActiveRole(user, RoleType.SUPER_ADMIN)) {
            throw new BadRequestException("Platform accounts must sign in through Thinkers Department");
        }

        return user;
    }

    private Optional<User> lookupOwnerInPublicSchema(String usernameOrEmail, Long organizationId) {
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            return publicSchemaUserLookupService.findActiveOwnerForOrganization(usernameOrEmail, organizationId);
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }

    /**
     * Runs the given action with {@link TenantContext} set to "public" — required before
     * calling any {@code REQUIRES_NEW} method on {@link PublicSchemaUserLookupService} (see
     * that class's Javadoc for why the switch must happen before, not inside, the call).
     */
    private void runInPublicSchema(Runnable action) {
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            action.run();
        } finally {
            TenantContext.setTenant(previousTenant);
        }
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
            TenantRegistry tenant = tenantRegistryRepository.findActiveByTenantIdentifierNormalized(loginContext.getTenantIdentifier())
                    .orElseThrow(() -> new BadRequestException("Unknown institution tenant"));
            // Eagerly touch org + customer while still on the platform catalog. After this
            // method restores the institution TenantContext, lazy-loading Customer would hit
            // the tenant DB (legacy customers schema) and fail.
            Organization organization = tenant.getOrganization();
            if (organization != null) {
                organization.getStatus();
                if (organization.getCustomer() != null) {
                    organization.getCustomer().getStatus();
                }
            }
            return tenant;
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
        boolean firstTimeLogin = Boolean.TRUE.equals(user.getFirstTimeLogin());
        claims.put("firstTimeLogin", firstTimeLogin);
        claims.put("requirePasswordChange", firstTimeLogin);

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
    public AuthResponse refreshToken(String refreshToken, String preferredTenant, Long preferredOrgId) {
        ResolvedRefreshSession resolved = resolveRefreshSession(refreshToken);
        UserSession session = resolved.session();
        User user = resolved.user();

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadRequestException("Session is no longer active");
        }

        String tenantId = resolveTenantForRefresh(user, preferredTenant);
        Long effectiveOrgId = resolveOrgForRefresh(user, preferredOrgId, tenantId);
        String loginContextValue = hasActiveRole(user, RoleType.SUPER_ADMIN)
                ? LoginContext.PLATFORM
                : LoginContext.TENANT;
        Map<String, Object> claims = buildTokenClaims(user, tenantId, loginContextValue, effectiveOrgId);

        String newAccessToken = jwtService.generateAccessToken(user.getUsername(), claims);
        Boolean rememberMe = jwtService.extractRememberMe(refreshToken);
        boolean remember = Boolean.TRUE.equals(rememberMe);
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername(), remember);

        if (resolved.publicSchemaSession()) {
            runInPublicSchema(() -> publicSchemaUserLookupService.rotateRefreshToken(session.getId(), newRefreshToken));
        } else {
            session.setRefreshToken(newRefreshToken);
            sessionRepository.save(session);
        }

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

    private ResolvedRefreshSession resolveRefreshSession(String refreshToken) {
        Optional<UserSession> ambient = sessionRepository.findByRefreshToken(refreshToken);
        if (ambient.isPresent()) {
            UserSession session = ambient.get();
            return new ResolvedRefreshSession(session, initializeUserRoles(session.getUser()), false);
        }
        String previousTenant = TenantContext.getTenant();
        try {
            TenantContext.setTenant("public");
            UserSession session = publicSchemaUserLookupService.findSessionByRefreshToken(refreshToken)
                    .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
            return new ResolvedRefreshSession(session, initializeUserRoles(session.getUser()), true);
        } finally {
            TenantContext.setTenant(previousTenant);
        }
    }

    private User initializeUserRoles(User user) {
        // Detached public-schema user must have roles initialized before role checks.
        user.getUserRoles().forEach(ur -> ur.getRole().getRoleType());
        return user;
    }

    private String resolveTenantForRefresh(User user, String preferredTenant) {
        String fallback = resolveTenantForUser(user);
        if (preferredTenant == null || preferredTenant.isBlank()) {
            return fallback;
        }
        String normalizedPreferred = preferredTenant.trim().toLowerCase().replace('-', '_');
        if (normalizedPreferred.equalsIgnoreCase(fallback)
                || LoginContext.PLATFORM_TENANT.equalsIgnoreCase(normalizedPreferred)) {
            return normalizedPreferred;
        }
        if (hasActiveRole(user, RoleType.ORGANIZATION_OWNER)) {
            boolean switchable = organizationRepository.findActiveByOwnerUserId(user.getId()).stream()
                    .map(Organization::getTenantRegistry)
                    .filter(tr -> tr != null && tr.getTenantIdentifier() != null)
                    .map(tr -> tr.getTenantIdentifier().trim().toLowerCase().replace('-', '_'))
                    .anyMatch(normalizedPreferred::equals);
            if (switchable) {
                return normalizedPreferred;
            }
        }
        return fallback;
    }

    private Long resolveOrgForRefresh(User user, Long preferredOrgId, String tenantId) {
        if (hasActiveRole(user, RoleType.SUPER_ADMIN)) {
            return null;
        }
        if (preferredOrgId != null && preferredOrgId > 0) {
            if (hasActiveRole(user, RoleType.ORGANIZATION_OWNER)) {
                boolean owned = organizationRepository.findActiveByOwnerUserId(user.getId()).stream()
                        .anyMatch(org -> preferredOrgId.equals(org.getId()));
                if (owned) {
                    return preferredOrgId;
                }
            } else if (preferredOrgId.equals(user.getOrganizationId())) {
                return preferredOrgId;
            }
        }
        if (hasActiveRole(user, RoleType.ORGANIZATION_OWNER)) {
            return organizationRepository.findActiveByOwnerUserId(user.getId()).stream()
                    .filter(org -> org.getTenantRegistry() != null
                            && tenantId != null
                            && tenantId.equalsIgnoreCase(org.getTenantRegistry().getTenantIdentifier()))
                    .map(Organization::getId)
                    .findFirst()
                    .orElseGet(() -> organizationRepository.findActiveByOwnerUserId(user.getId()).stream()
                            .map(Organization::getId)
                            .findFirst()
                            .orElse(user.getOrganizationId()));
        }
        return user.getOrganizationId();
    }

    private record ResolvedRefreshSession(UserSession session, User user, boolean publicSchemaSession) {}

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
