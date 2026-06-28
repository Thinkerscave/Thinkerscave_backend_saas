package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.ProvisionOrganizationRequest;
import com.thinkerscave.platform.dto.response.ProvisioningJobResponse;
import com.thinkerscave.platform.dto.response.ProvisioningResultResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationConfiguration;
import com.thinkerscave.platform.entity.OrganizationDomain;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.Promotion;
import com.thinkerscave.platform.entity.ProvisioningJob;
import com.thinkerscave.platform.entity.ProvisioningTemplate;
import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import com.thinkerscave.platform.entity.SubscriptionPlan;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.BillingCycle;
import com.thinkerscave.platform.enums.DomainStatus;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.platform.enums.ProvisionJobStatus;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.repository.OrganizationConfigurationRepository;
import com.thinkerscave.platform.repository.OrganizationDomainRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.repository.PromotionRepository;
import com.thinkerscave.platform.repository.ProvisioningJobRepository;
import com.thinkerscave.platform.repository.ProvisioningTemplateRepository;
import com.thinkerscave.platform.repository.SubscriptionFeatureOverrideRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.platform.service.ProvisionService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionServiceImpl implements ProvisionService {

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final TenantRegistryRepository tenantRepository;
    private final OrganizationDomainRepository domainRepository;
    private final OrganizationConfigurationRepository configRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PromotionRepository promotionRepository;
    private final FeatureRepository featureRepository;
    private final SubscriptionFeatureOverrideRepository overrideRepository;
    private final ProvisioningJobRepository jobRepository;
    private final ProvisioningTemplateRepository templateRepository;
    private final CodeGeneratorService codeGeneratorService;

    /**
     * Main provisioning workflow — orchestrates the complete 18-step process:
     * 1. Validate request
     * 2. Resolve / create Customer
     * 3. Generate Org Code
     * 4. Create Organization
     * 5. Create Provisioning Job (PENDING)
     * 6. Start Job — set IN_PROGRESS
     * 7. Create TenantRegistry
     * 8. Provision schema (dev: skip / prod: create PostgreSQL schema + Flyway)
     * 9. Create OrganizationDomain
     * 10. Create OrganizationConfiguration (defaults)
     * 11. Resolve Subscription Plan
     * 12. Apply Promotion (if provided)
     * 13. Create OrganizationSubscription
     * 14. Apply Feature Overrides (enabled / disabled list)
     * 15. Seed admin user (placeholder — actual seeding done by tenant-level setup)
     * 16. Mark Organization as onboarded
     * 17. Complete Job — set COMPLETED
     * 18. Return ProvisioningResultResponse
     */
    @Override
    @Transactional
    public ProvisioningResultResponse provision(ProvisionOrganizationRequest request) {
        LocalDateTime start = LocalDateTime.now();
        String provisionedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Starting provisioning by: {}", provisionedBy);

        // ── Step 1: Validate ──────────────────────────────────────────────────
        SubscriptionPlan plan = planRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan not found: " + request.getSubscriptionPlanId()));

        // ── Step 2: Resolve Customer ─────────────────────────────────────────
        Customer customer;
        if (request.getExistingCustomerId() != null) {
            customer = customerRepository.findById(request.getExistingCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getExistingCustomerId()));
        } else {
            if (request.getCustomerEmail() == null || request.getCustomerLegalName() == null) {
                throw new BadRequestException("Customer email and legal name are required when creating a new customer");
            }
            customer = customerRepository.findByEmail(request.getCustomerEmail())
                    .orElseGet(() -> {
                        String code = codeGeneratorService.generate(CodeType.CUSTOMER);
                        return customerRepository.save(Customer.builder()
                                .customerCode(code)
                                .legalName(request.getCustomerLegalName())
                                .displayName(request.getCustomerDisplayName() != null ? request.getCustomerDisplayName() : request.getCustomerLegalName())
                                .email(request.getCustomerEmail())
                                .mobileNumber(request.getCustomerMobile())
                                .active(true)
                                .onboardingCompleted(false)
                                .build());
                    });
        }

        // ── Steps 3 & 4: Create Organization ─────────────────────────────────
        String orgCode = codeGeneratorService.generate(CodeType.ORGANIZATION);
        Organization org = Organization.builder()
                .organizationCode(orgCode)
                .customer(customer)
                .organizationName(request.getOrganizationName())
                .shortName(request.getShortName())
                .institutionType(request.getInstitutionType())
                .boardName(request.getBoardName())
                .email(request.getOrgEmail())
                .mobileNumber(request.getOrgMobile())
                .addressLine1(request.getAddressLine1())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
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
        log.info("Organization created: {}", orgCode);

        // ── Steps 5 & 6: Create Job ───────────────────────────────────────────
        String jobCode = codeGeneratorService.generate(CodeType.PROVISION_JOB);
        ProvisioningTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId()).orElse(null);
        }
        ProvisioningJob job = ProvisioningJob.builder()
                .jobCode(jobCode)
                .organization(org)
                .provisioningTemplate(template)
                .status(ProvisionJobStatus.RUNNING)
                .currentStep("CREATING_TENANT")
                .progressPercentage(10)
                .startedAt(start)
                .provisionedBy(provisionedBy)
                .active(true)
                .build();
        job = jobRepository.save(job);

        try {
            // ── Step 7: Create TenantRegistry ────────────────────────────────
            String tenantId = codeGeneratorService.generate(CodeType.TENANT);
            String schemaName = "org_" + orgCode.toLowerCase();
            String subDomainSlug = request.getOrganizationName()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
            String tenantDomain = subDomainSlug + ".thinkerscave.app";

            TenantRegistry tenant = TenantRegistry.builder()
                    .tenantIdentifier(tenantId)
                    .organization(org)
                    .schemaName(schemaName)
                    .provisionStatus(ProvisionStatus.COMPLETED)
                    .tenantDomain(tenantDomain)
                    .maintenanceMode(false)
                    .active(true)
                    .build();
            tenant = tenantRepository.save(tenant);
            log.info("TenantRegistry created: {} -> schema: {}", tenantId, schemaName);

            // ── Step 8: Schema provisioning (dev placeholder) ─────────────────
            log.info("[DEV] Schema provisioning skipped for H2 dev profile. Schema: {}", schemaName);
            updateJobProgress(job, "SCHEMA_PROVISIONED", 30);

            // ── Step 9: Create Domain ─────────────────────────────────────────
            OrganizationDomain domain = OrganizationDomain.builder()
                    .organization(org)
                    .subDomain(subDomainSlug)
                    .domain(tenantDomain)
                    .sslEnabled(false)
                    .dnsVerified(false)
                    .defaultDomain(true)
                    .primaryDomain(true)
                    .status(DomainStatus.ACTIVE)
                    .active(true)
                    .build();
            domainRepository.save(domain);
            updateJobProgress(job, "DOMAIN_CONFIGURED", 45);

            // ── Step 10: Create Configuration ─────────────────────────────────
            OrganizationConfiguration config = OrganizationConfiguration.builder()
                    .organization(org)
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .timeZone(request.getTimeZone() != null ? request.getTimeZone() : "Asia/Kolkata")
                    .language(request.getLanguage() != null ? request.getLanguage() : "en")
                    .emailNotificationEnabled(true)
                    .smsNotificationEnabled(false)
                    .whatsappNotificationEnabled(false)
                    .allowSelfRegistration(false)
                    .multiBranchEnabled(false)
                    .maintenanceMode(false)
                    .active(true)
                    .build();
            configRepository.save(config);
            updateJobProgress(job, "CONFIGURATION_SEEDED", 55);

            // ── Steps 11–13: Create Subscription ─────────────────────────────
            LocalDate subStart = request.getSubscriptionStartDate() != null ? request.getSubscriptionStartDate() : LocalDate.now();
            BillingCycle billingCycle = request.getBillingCycle() != null ? request.getBillingCycle() : BillingCycle.MONTHLY;
            LocalDate subEnd = calculateEndDate(subStart, billingCycle);
            BigDecimal planPrice = resolvePlanPrice(plan, billingCycle);

            Promotion promotion = null;
            if (request.getPromotionId() != null) {
                promotion = promotionRepository.findById(request.getPromotionId()).orElse(null);
            } else if (request.getPromotionCode() != null) {
                promotion = promotionRepository.findByPromotionCode(request.getPromotionCode()).orElse(null);
            }

            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal finalAmount = planPrice != null ? planPrice : BigDecimal.ZERO;
            if (promotion != null && planPrice != null) {
                discountAmount = calculateDiscount(planPrice, promotion);
                finalAmount = planPrice.subtract(discountAmount).max(BigDecimal.ZERO);
                // Increment usage counter
                promotion.setUsedCount(promotion.getUsedCount() + 1);
                promotionRepository.save(promotion);
            }

            boolean trialEnabled = Boolean.TRUE.equals(request.getTrialEnabled()) && plan.getTrialDays() > 0;
            LocalDate trialEnd = trialEnabled ? subStart.plusDays(plan.getTrialDays()) : null;

            OrganizationSubscription subscription = OrganizationSubscription.builder()
                    .organization(org)
                    .subscriptionPlan(plan)
                    .promotion(promotion)
                    .startDate(subStart)
                    .endDate(subEnd)
                    .trialEndDate(trialEnd)
                    .billingCycle(billingCycle)
                    .planPrice(planPrice)
                    .discountAmount(discountAmount)
                    .finalAmount(finalAmount)
                    .studentLimitOverride(request.getStudentLimitOverride())
                    .staffLimitOverride(request.getStaffLimitOverride())
                    .branchLimitOverride(request.getBranchLimitOverride())
                    .storageLimitOverride(request.getStorageLimitOverride())
                    .autoRenew(false)
                    .status(trialEnabled ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE)
                    .active(true)
                    .build();
            subscription = subscriptionRepository.save(subscription);
            updateJobProgress(job, "SUBSCRIPTION_CREATED", 70);

            // ── Step 14: Apply Feature Overrides ──────────────────────────────
            final OrganizationSubscription savedSubscription = subscription;
            if (request.getDisabledFeatureIds() != null && !request.getDisabledFeatureIds().isEmpty()) {
                for (Long featureId : request.getDisabledFeatureIds()) {
                    featureRepository.findById(featureId).ifPresent(feature -> {
                        SubscriptionFeatureOverride override = SubscriptionFeatureOverride.builder()
                                .organizationSubscription(savedSubscription)
                                .feature(feature)
                                .enabled(false)
                                .overrideReason("Disabled during provisioning")
                                .active(true)
                                .build();
                        overrideRepository.save(override);
                    });
                }
            }
            updateJobProgress(job, "FEATURES_CONFIGURED", 80);

            // ── Step 15: Admin user (placeholder) ─────────────────────────────
            log.info("[TODO] Admin user creation for org {} will be handled during tenant-level bootstrap. Email: {}",
                    orgCode, request.getAdminEmail());
            updateJobProgress(job, "ADMIN_USER_CREATED", 90);

            // ── Step 16: Mark Organization as onboarded ───────────────────────
            org.setOnboardingCompleted(true);
            organizationRepository.save(org);
            customer.setOnboardingCompleted(true);
            customerRepository.save(customer);

            // ── Step 17: Complete Job ─────────────────────────────────────────
            LocalDateTime completedAt = LocalDateTime.now();
            job.setStatus(ProvisionJobStatus.COMPLETED);
            job.setCurrentStep("COMPLETED");
            job.setProgressPercentage(100);
            job.setCompletedAt(completedAt);
            job.setDurationSeconds(java.time.Duration.between(start, completedAt).getSeconds());
            job.setTenantRegistry(tenant);
            jobRepository.save(job);

            log.info("Provisioning completed: orgCode={}, tenantId={}, jobCode={}", orgCode, tenantId, jobCode);

            // ── Step 18: Return result ────────────────────────────────────────
            return ProvisioningResultResponse.builder()
                    .organizationId(org.getId())
                    .organizationCode(orgCode)
                    .organizationName(org.getOrganizationName())
                    .tenantId(tenant.getId())
                    .tenantIdentifier(tenantId)
                    .schemaName(schemaName)
                    .subscriptionId(subscription.getId())
                    .provisioningJobId(job.getId())
                    .jobCode(jobCode)
                    .adminEmail(request.getAdminEmail())
                    .defaultDomain(tenantDomain)
                    .message("Organization provisioned successfully. Admin user setup required.")
                    .build();

        } catch (Exception ex) {
            log.error("Provisioning failed for org: {}, error: {}", orgCode, ex.getMessage(), ex);
            job.setStatus(ProvisionJobStatus.FAILED);
            job.setCurrentStep("FAILED");
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProvisioningJobResponse> getJobs(ProvisionJobStatus status, String search, Pageable pageable) {
        return jobRepository.searchJobs(status, search, pageable).map(this::toJobResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningJobResponse getJobById(Long id) {
        return toJobResponse(findJobById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningJobResponse getJobLogs(Long id) {
        return toJobResponse(findJobById(id));
    }

    @Override
    @Transactional
    public ProvisioningJobResponse retryJob(Long id) {
        ProvisioningJob job = findJobById(id);
        if (job.getStatus() != ProvisionJobStatus.FAILED) {
            throw new BadRequestException("Only failed jobs can be retried");
        }
        job.setStatus(ProvisionJobStatus.PENDING);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(null);
        return toJobResponse(jobRepository.save(job));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateJobProgress(ProvisioningJob job, String step, int percentage) {
        job.setCurrentStep(step);
        job.setProgressPercentage(percentage);
        jobRepository.save(job);
    }

    private ProvisioningJob findJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProvisioningJob not found: " + id));
    }

    private BigDecimal resolvePlanPrice(SubscriptionPlan plan, BillingCycle cycle) {
        if (cycle == null) return plan.getMonthlyPrice();
        return switch (cycle) {
            case MONTHLY -> plan.getMonthlyPrice();
            case QUARTERLY -> plan.getQuarterlyPrice();
            case HALF_YEARLY -> plan.getHalfYearlyPrice();
            case YEARLY -> plan.getYearlyPrice();
            default -> plan.getMonthlyPrice();
        };
    }

    private LocalDate calculateEndDate(LocalDate start, BillingCycle cycle) {
        if (cycle == null) return start.plusMonths(1);
        return switch (cycle) {
            case MONTHLY -> start.plusMonths(1);
            case QUARTERLY -> start.plusMonths(3);
            case HALF_YEARLY -> start.plusMonths(6);
            case YEARLY -> start.plusYears(1);
            default -> start.plusMonths(1);
        };
    }

    private BigDecimal calculateDiscount(BigDecimal price, Promotion promotion) {
        return switch (promotion.getDiscountType()) {
            case PERCENTAGE -> price.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
            case FLAT_AMOUNT -> promotion.getDiscountValue().min(price);
        };
    }

    private ProvisioningJobResponse toJobResponse(ProvisioningJob j) {
        return ProvisioningJobResponse.builder()
                .id(j.getId())
                .jobCode(j.getJobCode())
                .organizationId(j.getOrganization().getId())
                .organizationName(j.getOrganization().getOrganizationName())
                .tenantRegistryId(j.getTenantRegistry() != null ? j.getTenantRegistry().getId() : null)
                .templateId(j.getProvisioningTemplate() != null ? j.getProvisioningTemplate().getId() : null)
                .templateName(j.getProvisioningTemplate() != null ? j.getProvisioningTemplate().getTemplateName() : null)
                .status(j.getStatus())
                .currentStep(j.getCurrentStep())
                .progressPercentage(j.getProgressPercentage())
                .startedAt(j.getStartedAt())
                .completedAt(j.getCompletedAt())
                .durationSeconds(j.getDurationSeconds())
                .retryCount(j.getRetryCount())
                .errorMessage(j.getErrorMessage())
                .provisionedBy(j.getProvisionedBy())
                .active(j.getActive())
                .remarks(j.getRemarks())
                .createdOn(j.getCreatedOn())
                .createdBy(j.getCreatedBy())
                .build();
    }
}
