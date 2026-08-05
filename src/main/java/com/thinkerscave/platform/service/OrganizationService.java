package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.OrganizationProfileUpdateRequest;
import com.thinkerscave.platform.dto.request.OrganizationRequest;
import com.thinkerscave.platform.dto.response.OrganizationDetailResponse;
import com.thinkerscave.platform.dto.response.OrganizationProfileResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.dto.response.PublicOrganizationOptionResponse;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrganizationService {

    Page<OrganizationSummaryResponse> getOrganizations(OrganizationStatus status, InstitutionType institutionType, Long customerId, String search, Pageable pageable);

    OrganizationDetailResponse getOrganizationById(Long id);

    OrganizationSummaryResponse createOrganization(OrganizationRequest request);

    OrganizationSummaryResponse updateOrganization(Long id, OrganizationRequest request);

    void archiveOrganization(Long id);

    OrganizationSummaryResponse activateOrganization(Long id);

    OrganizationSummaryResponse suspendOrganization(Long id);

    /** Active institutions for the unauthenticated org-select login screen. */
    List<PublicOrganizationOptionResponse> listPublicOrganizations(String search);

    /** Self-service profile read for the caller's own organization (Organization Admin/Owner). */
    OrganizationProfileResponse getMyOrganizationProfile(Long organizationId);

    /** Self-service profile update for the caller's own organization — Institution Type is never touched. */
    OrganizationProfileResponse updateMyOrganizationProfile(Long organizationId, OrganizationProfileUpdateRequest request);
}
