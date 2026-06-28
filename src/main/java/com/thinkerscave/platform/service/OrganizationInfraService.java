package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.DomainVerifyRequest;
import com.thinkerscave.platform.dto.request.OrganizationConfigurationRequest;
import com.thinkerscave.platform.dto.request.OrganizationDomainRequest;
import com.thinkerscave.platform.dto.response.OrganizationConfigurationResponse;
import com.thinkerscave.platform.dto.response.OrganizationDomainResponse;

import java.util.List;

public interface OrganizationInfraService {

    // Domains
    List<OrganizationDomainResponse> getAllDomains();

    OrganizationDomainResponse createDomain(OrganizationDomainRequest request);

    OrganizationDomainResponse updateDomain(Long id, OrganizationDomainRequest request);

    OrganizationDomainResponse verifyDomain(DomainVerifyRequest request);

    OrganizationDomainResponse testDomain(DomainVerifyRequest request);

    // Configuration
    OrganizationConfigurationResponse getConfiguration(Long organizationId);

    OrganizationConfigurationResponse updateConfiguration(Long organizationId, OrganizationConfigurationRequest request);
}
