package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.orgm.requestDto.OrgRegistrationRequest;
import com.thinkerscave.common.orgm.responseDto.OrgRegistrationResponse;

public interface OrganizationService {
    OrgRegistrationResponse registerOrg(OrgRegistrationRequest request);
}