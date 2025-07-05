package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.orgm.dto.OrgRegistrationRequest;
import com.thinkerscave.common.orgm.dto.OrgRegistrationResponse;

public interface OrganizationService {
    OrgRegistrationResponse registerOrg(OrgRegistrationRequest request);
}