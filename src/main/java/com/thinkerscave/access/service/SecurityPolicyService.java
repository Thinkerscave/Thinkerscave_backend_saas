package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.SecurityPolicyRequest;
import com.thinkerscave.access.dto.response.SecurityPolicyResponse;

/**
 * Organization-level security policy management.
 */
public interface SecurityPolicyService {

    SecurityPolicyResponse getPolicy(Long organizationId);

    SecurityPolicyResponse createOrUpdatePolicy(Long organizationId, SecurityPolicyRequest request);

    void resetToDefaults(Long organizationId);
}
