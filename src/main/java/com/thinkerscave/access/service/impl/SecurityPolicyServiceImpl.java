package com.thinkerscave.access.service.impl;

import com.thinkerscave.access.dto.request.SecurityPolicyRequest;
import com.thinkerscave.access.dto.response.SecurityPolicyResponse;
import com.thinkerscave.access.entity.SecurityPolicy;
import com.thinkerscave.access.mapper.SecurityPolicyMapper;
import com.thinkerscave.access.repository.SecurityPolicyRepository;
import com.thinkerscave.access.service.SecurityPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPolicyServiceImpl implements SecurityPolicyService {

    private final SecurityPolicyRepository securityPolicyRepository;
    private final SecurityPolicyMapper securityPolicyMapper;

    @Override
    @Transactional(readOnly = true)
    public SecurityPolicyResponse getPolicy(Long organizationId) {
        SecurityPolicy policy = securityPolicyRepository.findByOrganizationId(organizationId)
                .orElseGet(() -> SecurityPolicy.builder().organizationId(organizationId).build());
        return securityPolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional
    public SecurityPolicyResponse createOrUpdatePolicy(Long organizationId, SecurityPolicyRequest request) {
        SecurityPolicy policy = securityPolicyRepository.findByOrganizationId(organizationId)
                .orElse(SecurityPolicy.builder().organizationId(organizationId).build());

        policy.setMinPasswordLength(request.getMinPasswordLength());
        policy.setRequireUppercase(request.getRequireUppercase());
        policy.setRequireLowercase(request.getRequireLowercase());
        policy.setRequireNumbers(request.getRequireNumbers());
        policy.setRequireSpecialChars(request.getRequireSpecialChars());
        policy.setPasswordExpiryDays(request.getPasswordExpiryDays());
        policy.setPasswordHistoryCount(request.getPasswordHistoryCount());
        policy.setMaxFailedAttempts(request.getMaxFailedAttempts());
        policy.setLockoutDurationMinutes(request.getLockoutDurationMinutes());
        policy.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
        policy.setMaxConcurrentSessions(request.getMaxConcurrentSessions());
        policy.setAllowRememberMe(request.getAllowRememberMe());
        policy.setRequireTwoFactor(request.getRequireTwoFactor());
        policy.setActive(true);

        SecurityPolicy saved = securityPolicyRepository.save(policy);
        log.info("Security policy updated for org={}", organizationId);
        return securityPolicyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void resetToDefaults(Long organizationId) {
        securityPolicyRepository.findByOrganizationId(organizationId).ifPresent(policy -> {
            policy.setMinPasswordLength(8);
            policy.setRequireUppercase(true);
            policy.setRequireLowercase(true);
            policy.setRequireNumbers(true);
            policy.setRequireSpecialChars(false);
            policy.setPasswordExpiryDays(90);
            policy.setPasswordHistoryCount(5);
            policy.setMaxFailedAttempts(5);
            policy.setLockoutDurationMinutes(30);
            policy.setSessionTimeoutMinutes(60);
            policy.setMaxConcurrentSessions(3);
            policy.setAllowRememberMe(false);
            policy.setRequireTwoFactor(false);
            securityPolicyRepository.save(policy);
            log.info("Security policy reset to defaults for org={}", organizationId);
        });
    }
}
