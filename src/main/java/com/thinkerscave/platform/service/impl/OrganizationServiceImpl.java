package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.OrganizationRequest;
import com.thinkerscave.platform.dto.response.OrganizationConfigurationResponse;
import com.thinkerscave.platform.dto.response.OrganizationDetailResponse;
import com.thinkerscave.platform.dto.response.OrganizationDomainResponse;
import com.thinkerscave.platform.dto.response.OrganizationSubscriptionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.dto.response.TenantRegistryResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationSummaryResponse> getOrganizations(OrganizationStatus status, InstitutionType institutionType, Long customerId, String search, Pageable pageable) {
        return organizationRepository.searchOrganizations(status, institutionType, customerId, search, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationDetailResponse getOrganizationById(Long id) {
        Organization org = findById(id);
        return toDetailResponse(org);
    }

    @Override
    @Transactional
    public OrganizationSummaryResponse createOrganization(OrganizationRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
        String code = codeGeneratorService.generate(CodeType.ORGANIZATION);
        Organization org = Organization.builder()
                .organizationCode(code)
                .customer(customer)
                .organizationName(request.getOrganizationName())
                .shortName(request.getShortName())
                .institutionType(request.getInstitutionType())
                .boardName(request.getBoardName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .alternateMobileNumber(request.getAlternateMobileNumber())
                .website(request.getWebsite())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .timeZone(request.getTimeZone())
                .currency(request.getCurrency())
                .language(request.getLanguage())
                .logoUrl(request.getLogoUrl())
                .status(OrganizationStatus.ACTIVE)
                .onboardingCompleted(false)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        org = organizationRepository.save(org);
        log.info("Organization created: {}", code);
        return toSummaryResponse(org);
    }

    @Override
    @Transactional
    public OrganizationSummaryResponse updateOrganization(Long id, OrganizationRequest request) {
        Organization org = findById(id);
        org.setOrganizationName(request.getOrganizationName());
        org.setShortName(request.getShortName());
        org.setInstitutionType(request.getInstitutionType());
        org.setBoardName(request.getBoardName());
        org.setEmail(request.getEmail());
        org.setMobileNumber(request.getMobileNumber());
        org.setAlternateMobileNumber(request.getAlternateMobileNumber());
        org.setWebsite(request.getWebsite());
        org.setAddressLine1(request.getAddressLine1());
        org.setAddressLine2(request.getAddressLine2());
        org.setCity(request.getCity());
        org.setState(request.getState());
        org.setCountry(request.getCountry());
        org.setPostalCode(request.getPostalCode());
        org.setTimeZone(request.getTimeZone());
        org.setCurrency(request.getCurrency());
        org.setLanguage(request.getLanguage());
        org.setLogoUrl(request.getLogoUrl());
        org.setRemarks(request.getRemarks());
        return toSummaryResponse(organizationRepository.save(org));
    }

    @Override
    @Transactional
    public void archiveOrganization(Long id) {
        Organization org = findById(id);
        if (org.getStatus() == OrganizationStatus.ACTIVE) {
            throw new BadRequestException("Cannot archive an active organization. Suspend it first.");
        }
        org.setActive(false);
        org.setStatus(OrganizationStatus.ARCHIVED);
        organizationRepository.save(org);
        log.info("Organization archived: {}", org.getOrganizationCode());
    }

    @Override
    @Transactional
    public OrganizationSummaryResponse activateOrganization(Long id) {
        Organization org = findById(id);
        org.setStatus(OrganizationStatus.ACTIVE);
        org.setActive(true);
        return toSummaryResponse(organizationRepository.save(org));
    }

    @Override
    @Transactional
    public OrganizationSummaryResponse suspendOrganization(Long id) {
        Organization org = findById(id);
        org.setStatus(OrganizationStatus.SUSPENDED);
        return toSummaryResponse(organizationRepository.save(org));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public Organization findById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + id));
    }

    public OrganizationSummaryResponse toSummaryResponse(Organization o) {
        return OrganizationSummaryResponse.builder()
                .id(o.getId())
                .organizationCode(o.getOrganizationCode())
                .organizationName(o.getOrganizationName())
                .shortName(o.getShortName())
                .institutionType(o.getInstitutionType())
                .status(o.getStatus())
                .email(o.getEmail())
                .mobileNumber(o.getMobileNumber())
                .city(o.getCity())
                .state(o.getState())
                .country(o.getCountry())
                .logoUrl(o.getLogoUrl())
                .onboardingCompleted(o.getOnboardingCompleted())
                .active(o.getActive())
                .tenantIdentifier(o.getTenantRegistry() != null ? o.getTenantRegistry().getTenantIdentifier() : null)
                .createdOn(o.getCreatedOn())
                .build();
    }

    private OrganizationDetailResponse toDetailResponse(Organization o) {
        TenantRegistryResponse tenantResponse = null;
        if (o.getTenantRegistry() != null) {
            var t = o.getTenantRegistry();
            tenantResponse = TenantRegistryResponse.builder()
                    .id(t.getId())
                    .tenantIdentifier(t.getTenantIdentifier())
                    .organizationId(o.getId())
                    .organizationName(o.getOrganizationName())
                    .schemaName(t.getSchemaName())
                    .provisionStatus(t.getProvisionStatus())
                    .databaseVersion(t.getDatabaseVersion())
                    .migrationVersion(t.getMigrationVersion())
                    .templateVersion(t.getTemplateVersion())
                    .databaseSizeMb(t.getDatabaseSizeMb())
                    .storageUsedMb(t.getStorageUsedMb())
                    .lastMigrationAt(t.getLastMigrationAt())
                    .lastBackupAt(t.getLastBackupAt())
                    .lastHealthCheckAt(t.getLastHealthCheckAt())
                    .tenantDomain(t.getTenantDomain())
                    .customDomain(t.getCustomDomain())
                    .maintenanceMode(t.getMaintenanceMode())
                    .active(t.getActive())
                    .createdOn(t.getCreatedOn())
                    .createdBy(t.getCreatedBy())
                    .build();
        }

        OrganizationDomainResponse domainResponse = null;
        if (o.getOrganizationDomain() != null) {
            var d = o.getOrganizationDomain();
            domainResponse = OrganizationDomainResponse.builder()
                    .id(d.getId())
                    .organizationId(o.getId())
                    .organizationName(o.getOrganizationName())
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
                    .createdOn(d.getCreatedOn())
                    .build();
        }

        OrganizationSubscriptionResponse subscriptionResponse = null;
        if (o.getOrganizationSubscription() != null) {
            var s = o.getOrganizationSubscription();
            subscriptionResponse = OrganizationSubscriptionResponse.builder()
                    .id(s.getId())
                    .organizationId(o.getId())
                    .organizationName(o.getOrganizationName())
                    .organizationCode(o.getOrganizationCode())
                    .subscriptionPlanId(s.getSubscriptionPlan().getId())
                    .planCode(s.getSubscriptionPlan().getPlanCode())
                    .planName(s.getSubscriptionPlan().getPlanName())
                    .promotionId(s.getPromotion() != null ? s.getPromotion().getId() : null)
                    .promotionCode(s.getPromotion() != null ? s.getPromotion().getPromotionCode() : null)
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .trialEndDate(s.getTrialEndDate())
                    .billingCycle(s.getBillingCycle())
                    .planPrice(s.getPlanPrice())
                    .discountAmount(s.getDiscountAmount())
                    .finalAmount(s.getFinalAmount())
                    .studentLimitOverride(s.getStudentLimitOverride())
                    .staffLimitOverride(s.getStaffLimitOverride())
                    .branchLimitOverride(s.getBranchLimitOverride())
                    .storageLimitOverride(s.getStorageLimitOverride())
                    .autoRenew(s.getAutoRenew())
                    .status(s.getStatus())
                    .active(s.getActive())
                    .createdOn(s.getCreatedOn())
                    .build();
        }

        OrganizationConfigurationResponse configResponse = null;
        if (o.getOrganizationConfiguration() != null) {
            var c = o.getOrganizationConfiguration();
            configResponse = OrganizationConfigurationResponse.builder()
                    .id(c.getId())
                    .organizationId(o.getId())
                    .defaultAcademicYear(c.getDefaultAcademicYear())
                    .academicYearStartMonth(c.getAcademicYearStartMonth())
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
                    .createdOn(c.getCreatedOn())
                    .build();
        }

        return OrganizationDetailResponse.builder()
                .id(o.getId())
                .organizationCode(o.getOrganizationCode())
                .organizationName(o.getOrganizationName())
                .shortName(o.getShortName())
                .institutionType(o.getInstitutionType())
                .boardName(o.getBoardName())
                .status(o.getStatus())
                .email(o.getEmail())
                .mobileNumber(o.getMobileNumber())
                .alternateMobileNumber(o.getAlternateMobileNumber())
                .website(o.getWebsite())
                .addressLine1(o.getAddressLine1())
                .addressLine2(o.getAddressLine2())
                .city(o.getCity())
                .state(o.getState())
                .country(o.getCountry())
                .postalCode(o.getPostalCode())
                .timeZone(o.getTimeZone())
                .currency(o.getCurrency())
                .language(o.getLanguage())
                .logoUrl(o.getLogoUrl())
                .onboardingCompleted(o.getOnboardingCompleted())
                .active(o.getActive())
                .remarks(o.getRemarks())
                .customerId(o.getCustomer().getId())
                .customerCode(o.getCustomer().getCustomerCode())
                .customerName(o.getCustomer().getDisplayName())
                .tenant(tenantResponse)
                .domain(domainResponse)
                .subscription(subscriptionResponse)
                .configuration(configResponse)
                .createdOn(o.getCreatedOn())
                .createdBy(o.getCreatedBy())
                .updatedOn(o.getUpdatedOn())
                .updatedBy(o.getUpdatedBy())
                .build();
    }
}
