package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.OrganizationRequest;
import com.thinkerscave.platform.dto.request.OrganizationProfileUpdateRequest;
import com.thinkerscave.platform.dto.response.FeatureOverrideResponse;
import com.thinkerscave.platform.dto.response.OrganizationConfigurationResponse;
import com.thinkerscave.platform.dto.response.OrganizationDetailResponse;
import com.thinkerscave.platform.dto.response.OrganizationDomainResponse;
import com.thinkerscave.platform.dto.response.OrganizationProfileResponse;
import com.thinkerscave.platform.dto.response.OrganizationSubscriptionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.dto.response.PublicOrganizationOptionResponse;
import com.thinkerscave.platform.dto.response.SubscriptionPlanFeatureResponse;
import com.thinkerscave.platform.dto.response.TenantRegistryResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.entity.SubscriptionPlanFeature;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.SubscriptionFeatureOverrideRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.service.OrganizationInvoicePdfService;
import com.thinkerscave.platform.service.OrganizationService;
import com.thinkerscave.platform.service.TenantInsightService;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CustomerRepository customerRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final TenantInsightService tenantInsightService;
    private final SubscriptionPlanFeatureRepository planFeatureRepository;
    private final SubscriptionFeatureOverrideRepository overrideRepository;
    private final OrganizationInvoicePdfService organizationInvoicePdfService;

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationSummaryResponse> getOrganizations(OrganizationStatus status, InstitutionType institutionType, Long customerId, String search, Pageable pageable) {
        return organizationRepository.searchOrganizations(status, institutionType, customerId, search, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    @Transactional
    public OrganizationDetailResponse getOrganizationById(Long id) {
        Organization org = findById(id);
        if (org.getTenantRegistry() != null) {
            tenantInsightService.refreshIfStale(org.getTenantRegistry());
        }
        return toDetailResponse(org);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadOnboardingInvoicePdf(Long id) {
        Organization org = findById(id);
        OrganizationSubscription subscription = org.getOrganizationSubscription();
        if (subscription == null) {
            throw new BadRequestException("No subscription invoice is available for this organization.");
        }
        if (subscription.getSubscriptionPlan() != null) {
            subscription.getSubscriptionPlan().getPlanName();
        }
        if (subscription.getPromotion() != null) {
            subscription.getPromotion().getPromotionCode();
        }
        if (org.getCustomer() != null) {
            org.getCustomer().getCustomerName();
        }
        return organizationInvoicePdfService.build(org, subscription);
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
        if (StringUtils.hasText(request.getAdminFullName())) {
            org.setAdminFullName(request.getAdminFullName().trim());
        }
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
        Organization saved = organizationRepository.save(org);
        if (saved.getTenantRegistry() != null) {
            tenantInsightService.updateOrgAdmin(
                    saved.getTenantRegistry().getSchemaName(),
                    saved.getAdminFullName(),
                    saved.getEmail(),
                    saved.getMobileNumber());
        }
        return toSummaryResponse(saved);
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
    @Override
    @Transactional(readOnly = true)
    public OrganizationProfileResponse getMyOrganizationProfile(Long organizationId) {
        return toProfileResponse(findById(organizationId));
    }

    @Override
    @Transactional
    public OrganizationProfileResponse updateMyOrganizationProfile(Long organizationId, OrganizationProfileUpdateRequest request) {
        Organization org = findById(organizationId);
        org.setOrganizationName(request.getOrganizationName());
        org.setShortName(request.getShortName());
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
        org.setLogoUrl(request.getLogoUrl());
        // institutionType is intentionally never touched here — not editable via self-service profile.
        return toProfileResponse(organizationRepository.save(org));
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PublicOrganizationOptionResponse> listPublicOrganizations(String search) {
        String normalized = StringUtils.hasText(search) ? search.trim() : null;
        return organizationRepository.findPublicLoginOrganizations(normalized).stream()
                .filter(o -> o.getTenantRegistry() != null
                        && StringUtils.hasText(o.getTenantRegistry().getTenantIdentifier()))
                .map(this::toPublicOption)
                .sorted(Comparator.comparing(PublicOrganizationOptionResponse::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    private OrganizationProfileResponse toProfileResponse(Organization o) {
        return OrganizationProfileResponse.builder()
                .id(o.getId())
                .organizationCode(o.getOrganizationCode())
                .organizationName(o.getOrganizationName())
                .shortName(o.getShortName())
                .institutionType(o.getInstitutionType())
                .boardName(o.getBoardName())
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
                .logoUrl(o.getLogoUrl())
                .build();
    }

    private PublicOrganizationOptionResponse toPublicOption(Organization o) {
        String location = Stream.of(o.getCity(), o.getState(), o.getCountry())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
        return PublicOrganizationOptionResponse.builder()
                .id(o.getId())
                .name(o.getOrganizationName())
                .tenantId(o.getTenantRegistry().getTenantIdentifier())
                .location(StringUtils.hasText(location) ? location : "Institution")
                .logoUrl(o.getLogoUrl())
                .institutionType(o.getInstitutionType() != null ? o.getInstitutionType().name() : null)
                .build();
    }

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
        TenantRegistry tenant = o.getTenantRegistry();
        TenantInsightService.OrgAdmin admin = tenant != null
                ? tenantInsightService.findOrgAdmin(tenant.getSchemaName())
                : null;
        String adminEmail = firstText(o.getEmail(), admin != null ? admin.email() : null);
        String adminMobile = firstText(o.getMobileNumber(), admin != null ? admin.mobile() : null);
        String adminName = firstText(o.getAdminFullName(), admin != null ? admin.fullName() : null);

        TenantRegistryResponse tenantResponse = null;
        if (tenant != null) {
            tenantResponse = TenantRegistryResponse.builder()
                    .id(tenant.getId())
                    .tenantIdentifier(tenant.getTenantIdentifier())
                    .organizationId(o.getId())
                    .organizationName(o.getOrganizationName())
                    .schemaName(tenant.getSchemaName())
                    .provisionStatus(tenant.getProvisionStatus())
                    .databaseVersion(tenant.getDatabaseVersion())
                    .migrationVersion(tenant.getMigrationVersion())
                    .templateVersion(tenant.getTemplateVersion())
                    .databaseSizeMb(tenant.getDatabaseSizeMb())
                    .storageUsedMb(tenant.getStorageUsedMb())
                    .studentCount(tenant.getStudentCount())
                    .staffCount(tenant.getStaffCount())
                    .branchCount(tenant.getBranchCount())
                    .classCount(tenant.getClassCount())
                    .sectionCount(tenant.getSectionCount())
                    .usageRefreshedAt(tenant.getUsageRefreshedAt())
                    .lastMigrationAt(tenant.getLastMigrationAt())
                    .lastBackupAt(tenant.getLastBackupAt())
                    .lastHealthCheckAt(tenant.getLastHealthCheckAt())
                    .tenantDomain(tenant.getTenantDomain())
                    .customDomain(tenant.getCustomDomain())
                    .maintenanceMode(tenant.getMaintenanceMode())
                    .active(tenant.getActive())
                    .createdOn(tenant.getCreatedOn())
                    .createdBy(tenant.getCreatedBy())
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
        List<SubscriptionPlanFeatureResponse> entitled = List.of();
        if (o.getOrganizationSubscription() != null) {
            var s = o.getOrganizationSubscription();
            SubscriptionPlan plan = s.getSubscriptionPlan();
            Integer studentLimit = firstInt(s.getStudentLimitOverride(), plan != null ? plan.getStudentLimit() : null);
            Integer staffLimit = firstInt(s.getStaffLimitOverride(), plan != null ? plan.getStaffLimit() : null);
            Integer branchLimit = firstInt(s.getBranchLimitOverride(), plan != null ? plan.getBranchLimit() : null);
            Integer storageLimit = firstInt(s.getStorageLimitOverride(), plan != null ? plan.getStorageLimitGb() : null);
            List<FeatureOverrideResponse> overrides = overrideRepository
                    .findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(s.getId())
                    .stream().map(this::toOverrideResponse).collect(Collectors.toList());
            entitled = entitledFeatures(plan != null ? plan.getId() : null, s.getId());
            subscriptionResponse = OrganizationSubscriptionResponse.builder()
                    .id(s.getId())
                    .organizationId(o.getId())
                    .organizationName(o.getOrganizationName())
                    .organizationCode(o.getOrganizationCode())
                    .subscriptionPlanId(plan != null ? plan.getId() : null)
                    .planCode(plan != null ? plan.getPlanCode() : null)
                    .planName(plan != null ? plan.getPlanName() : null)
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
                    .studentLimit(studentLimit)
                    .staffLimit(staffLimit)
                    .branchLimit(branchLimit)
                    .storageLimitGb(storageLimit)
                    .invoiceNumber(OrganizationInvoicePdfService.invoiceNumber(o.getOrganizationCode(), s.getId()))
                    .autoRenew(s.getAutoRenew())
                    .status(s.getStatus())
                    .active(s.getActive())
                    .featureOverrides(overrides)
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
                .email(adminEmail)
                .mobileNumber(adminMobile)
                .adminFullName(adminName)
                .adminEmail(adminEmail)
                .adminMobile(adminMobile)
                .alternateMobileNumber(o.getAlternateMobileNumber())
                .website(firstText(
                        o.getWebsite(),
                        o.getOrganizationDomain() != null ? o.getOrganizationDomain().getDomain() : null,
                        tenant != null ? tenant.getTenantDomain() : null))
                .addressLine1(firstText(o.getAddressLine1(), locationLabel(o)))
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
                .customerName(o.getCustomer().getCustomerName())
                .tenant(tenantResponse)
                .domain(domainResponse)
                .subscription(subscriptionResponse)
                .entitledFeatures(entitled)
                .configuration(configResponse)
                .createdOn(o.getCreatedOn())
                .createdBy(o.getCreatedBy())
                .updatedOn(o.getUpdatedOn())
                .updatedBy(o.getUpdatedBy())
                .build();
    }

    private List<SubscriptionPlanFeatureResponse> entitledFeatures(Long planId, Long subscriptionId) {
        Map<Long, SubscriptionPlanFeatureResponse> byFeature = new LinkedHashMap<>();
        if (planId != null) {
            for (SubscriptionPlanFeature spf : planFeatureRepository
                    .findBySubscriptionPlan_IdAndEnabledTrueAndActiveTrue(planId)) {
                byFeature.put(spf.getFeature().getId(), toPlanFeatureResponse(spf, true));
            }
        }
        if (subscriptionId != null) {
            for (SubscriptionFeatureOverride override : overrideRepository
                    .findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(subscriptionId)) {
                Long featureId = override.getFeature().getId();
                if (Boolean.FALSE.equals(override.getEnabled())) {
                    byFeature.remove(featureId);
                } else {
                    byFeature.put(featureId, SubscriptionPlanFeatureResponse.builder()
                            .featureId(featureId)
                            .featureCode(override.getFeature().getFeatureCode())
                            .featureName(override.getFeature().getFeatureName())
                            .featureKey(override.getFeature().getFeatureKey())
                            .module(override.getFeature().getModule())
                            .enabled(true)
                            .notes("Override")
                            .active(true)
                            .build());
                }
            }
        }
        return new ArrayList<>(byFeature.values());
    }

    private SubscriptionPlanFeatureResponse toPlanFeatureResponse(SubscriptionPlanFeature spf, boolean enabled) {
        return SubscriptionPlanFeatureResponse.builder()
                .id(spf.getId())
                .subscriptionPlanId(spf.getSubscriptionPlan().getId())
                .planName(spf.getSubscriptionPlan().getPlanName())
                .featureId(spf.getFeature().getId())
                .featureCode(spf.getFeature().getFeatureCode())
                .featureName(spf.getFeature().getFeatureName())
                .featureKey(spf.getFeature().getFeatureKey())
                .module(spf.getFeature().getModule())
                .enabled(enabled)
                .mandatory(spf.getMandatory())
                .displayOrder(spf.getDisplayOrder())
                .notes(spf.getNotes())
                .active(spf.getActive())
                .createdOn(spf.getCreatedOn())
                .build();
    }

    private FeatureOverrideResponse toOverrideResponse(SubscriptionFeatureOverride override) {
        return FeatureOverrideResponse.builder()
                .id(override.getId())
                .organizationSubscriptionId(override.getOrganizationSubscription().getId())
                .featureId(override.getFeature().getId())
                .featureCode(override.getFeature().getFeatureCode())
                .featureName(override.getFeature().getFeatureName())
                .featureKey(override.getFeature().getFeatureKey())
                .enabled(override.getEnabled())
                .overrideReason(override.getOverrideReason())
                .expiryDate(override.getExpiryDate())
                .complimentary(override.getComplimentary())
                .chargeable(override.getChargeable())
                .additionalCharge(override.getAdditionalCharge())
                .active(override.getActive())
                .remarks(override.getRemarks())
                .createdOn(override.getCreatedOn())
                .createdBy(override.getCreatedBy())
                .build();
    }

    private static Integer firstInt(Integer... values) {
        return Stream.of(values).filter(v -> v != null).findFirst().orElse(null);
    }

    private static String locationLabel(Organization o) {
        return Stream.of(o.getCity(), o.getState(), o.getCountry())
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(", "));
    }

    private static String firstText(String... values) {
        return Stream.of(values)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }
}
