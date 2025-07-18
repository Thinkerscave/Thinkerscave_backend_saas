package com.thinkerscave.common.orgm.service;


import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.dto.OrgRegistrationRequest;
import com.thinkerscave.common.orgm.dto.OrgRegistrationResponse;

import java.util.List;

public interface OrganizationService {
    OrgRegistrationResponse registerOrg(OrgRegistrationRequest request);
    List<Organisation> getAllOrgs();
}