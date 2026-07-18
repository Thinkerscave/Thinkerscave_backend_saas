package com.thinkerscave.security.service;

import com.thinkerscave.security.dto.response.WorkspaceOrganizationResponse;

import java.util.List;

public interface WorkspaceAccessService {

    List<WorkspaceOrganizationResponse> getOwnedOrganizations();

    WorkspaceOrganizationResponse switchWorkspace(Long organizationId);
}
