package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.DomainVerifyRequest;
import com.thinkerscave.platform.dto.request.OrganizationConfigurationRequest;
import com.thinkerscave.platform.dto.request.OrganizationDomainRequest;
import com.thinkerscave.platform.dto.response.OrganizationConfigurationResponse;
import com.thinkerscave.platform.dto.response.OrganizationDomainResponse;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationConfiguration;
import com.thinkerscave.platform.entity.OrganizationDomain;
import com.thinkerscave.platform.enums.DomainStatus;
import com.thinkerscave.platform.repository.OrganizationConfigurationRepository;
import com.thinkerscave.platform.repository.OrganizationDomainRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.OrganizationInfraService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationInfraServiceImpl implements OrganizationInfraService {

    private final OrganizationDomainRepository domainRepository;
    private final OrganizationConfigurationRepository configRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationDomainResponse> getAllDomains() {
        return domainRepository.findAll().stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .map(this::toDomainResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrganizationDomainResponse createDomain(OrganizationDomainRequest request) {
        Organization org = findOrg(request.getOrganizationId());
        if (domainRepository.existsBySubDomain(request.getSubDomain())) {
            throw new AlreadyExistsException("Subdomain already exists: " + request.getSubDomain());
        }
        String domain = request.getSubDomain() + ".thinkerscave.app";
        OrganizationDomain od = OrganizationDomain.builder()
                .organization(org)
                .subDomain(request.getSubDomain())
                .domain(domain)
                .customDomain(request.getCustomDomain())
                .sslEnabled(Boolean.TRUE.equals(request.getSslEnabled()))
                .dnsVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .defaultDomain(true)
                .primaryDomain(true)
                .status(DomainStatus.ACTIVE)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toDomainResponse(domainRepository.save(od));
    }

    @Override
    @Transactional
    public OrganizationDomainResponse updateDomain(Long id, OrganizationDomainRequest request) {
        OrganizationDomain od = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationDomain not found: " + id));
        od.setCustomDomain(request.getCustomDomain());
        if (request.getSslEnabled() != null) od.setSslEnabled(request.getSslEnabled());
        od.setRemarks(request.getRemarks());
        return toDomainResponse(domainRepository.save(od));
    }

    @Override
    @Transactional
    public OrganizationDomainResponse verifyDomain(DomainVerifyRequest request) {
        OrganizationDomain od = domainRepository.findBySubDomain(request.getDomain())
                .or(() -> domainRepository.findByCustomDomain(request.getDomain()))
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found: " + request.getDomain()));
        // In production, this would make a real DNS lookup. For dev, we mark verified.
        od.setDnsVerified(true);
        od.setStatus(DomainStatus.ACTIVE);
        log.info("[DEV] Domain verified (simulated): {}", request.getDomain());
        return toDomainResponse(domainRepository.save(od));
    }

    @Override
    @Transactional
    public OrganizationDomainResponse testDomain(DomainVerifyRequest request) {
        OrganizationDomain od = domainRepository.findBySubDomain(request.getDomain())
                .or(() -> domainRepository.findByCustomDomain(request.getDomain()))
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found: " + request.getDomain()));
        log.info("[DEV] Domain test (simulated): {}", request.getDomain());
        return toDomainResponse(od);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationConfigurationResponse getConfiguration(Long organizationId) {
        OrganizationConfiguration config = configRepository.findByOrganization_Id(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuration not found for organization: " + organizationId));
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public OrganizationConfigurationResponse updateConfiguration(Long organizationId, OrganizationConfigurationRequest request) {
        OrganizationConfiguration config = configRepository.findByOrganization_Id(organizationId)
                .orElseGet(() -> {
                    Organization org = findOrg(organizationId);
                    return OrganizationConfiguration.builder()
                            .organization(org)
                            .active(true)
                            .build();
                });
        if (request.getDefaultAcademicYear() != null) config.setDefaultAcademicYear(request.getDefaultAcademicYear());
        if (request.getAcademicYearStartMonth() != null) config.setAcademicYearStartMonth(request.getAcademicYearStartMonth());
        if (request.getStudentCodePattern() != null) config.setStudentCodePattern(request.getStudentCodePattern());
        if (request.getEmployeeCodePattern() != null) config.setEmployeeCodePattern(request.getEmployeeCodePattern());
        if (request.getAdmissionNumberPattern() != null) config.setAdmissionNumberPattern(request.getAdmissionNumberPattern());
        if (request.getReceiptNumberPattern() != null) config.setReceiptNumberPattern(request.getReceiptNumberPattern());
        if (request.getInvoiceNumberPattern() != null) config.setInvoiceNumberPattern(request.getInvoiceNumberPattern());
        if (request.getCurrency() != null) config.setCurrency(request.getCurrency());
        if (request.getTimeZone() != null) config.setTimeZone(request.getTimeZone());
        if (request.getLanguage() != null) config.setLanguage(request.getLanguage());
        if (request.getDateFormat() != null) config.setDateFormat(request.getDateFormat());
        if (request.getTimeFormat() != null) config.setTimeFormat(request.getTimeFormat());
        if (request.getEmailNotificationEnabled() != null) config.setEmailNotificationEnabled(request.getEmailNotificationEnabled());
        if (request.getSmsNotificationEnabled() != null) config.setSmsNotificationEnabled(request.getSmsNotificationEnabled());
        if (request.getWhatsappNotificationEnabled() != null) config.setWhatsappNotificationEnabled(request.getWhatsappNotificationEnabled());
        if (request.getAllowSelfRegistration() != null) config.setAllowSelfRegistration(request.getAllowSelfRegistration());
        if (request.getMultiBranchEnabled() != null) config.setMultiBranchEnabled(request.getMultiBranchEnabled());
        if (request.getMaintenanceMode() != null) config.setMaintenanceMode(request.getMaintenanceMode());
        config.setRemarks(request.getRemarks());
        return toConfigResponse(configRepository.save(config));
    }

    private Organization findOrg(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
    }

    private OrganizationDomainResponse toDomainResponse(OrganizationDomain d) {
        return OrganizationDomainResponse.builder()
                .id(d.getId())
                .organizationId(d.getOrganization().getId())
                .organizationName(d.getOrganization().getOrganizationName())
                .subDomain(d.getSubDomain())
                .domain(d.getDomain())
                .customDomain(d.getCustomDomain())
                .sslEnabled(d.getSslEnabled())
                .sslProvider(d.getSslProvider())
                .sslExpiry(d.getSslExpiry())
                .dnsVerified(d.getDnsVerified())
                .verificationToken(d.getVerificationToken())
                .defaultDomain(d.getDefaultDomain())
                .primaryDomain(d.getPrimaryDomain())
                .status(d.getStatus())
                .active(d.getActive())
                .remarks(d.getRemarks())
                .createdOn(d.getCreatedOn())
                .createdBy(d.getCreatedBy())
                .updatedOn(d.getUpdatedOn())
                .updatedBy(d.getUpdatedBy())
                .build();
    }

    private OrganizationConfigurationResponse toConfigResponse(OrganizationConfiguration c) {
        return OrganizationConfigurationResponse.builder()
                .id(c.getId())
                .organizationId(c.getOrganization().getId())
                .defaultAcademicYear(c.getDefaultAcademicYear())
                .academicYearStartMonth(c.getAcademicYearStartMonth())
                .studentCodePattern(c.getStudentCodePattern())
                .employeeCodePattern(c.getEmployeeCodePattern())
                .admissionNumberPattern(c.getAdmissionNumberPattern())
                .receiptNumberPattern(c.getReceiptNumberPattern())
                .invoiceNumberPattern(c.getInvoiceNumberPattern())
                .currency(c.getCurrency())
                .timeZone(c.getTimeZone())
                .language(c.getLanguage())
                .dateFormat(c.getDateFormat())
                .timeFormat(c.getTimeFormat())
                .emailNotificationEnabled(c.getEmailNotificationEnabled())
                .smsNotificationEnabled(c.getSmsNotificationEnabled())
                .whatsappNotificationEnabled(c.getWhatsappNotificationEnabled())
                .allowSelfRegistration(c.getAllowSelfRegistration())
                .multiBranchEnabled(c.getMultiBranchEnabled())
                .maintenanceMode(c.getMaintenanceMode())
                .active(c.getActive())
                .remarks(c.getRemarks())
                .createdOn(c.getCreatedOn())
                .createdBy(c.getCreatedBy())
                .updatedOn(c.getUpdatedOn())
                .updatedBy(c.getUpdatedBy())
                .build();
    }
}
