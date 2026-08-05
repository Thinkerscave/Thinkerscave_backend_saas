package com.thinkerscave.platform.service.impl;

import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.enums.UserStatus;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.platform.dto.request.ProvisionOrganizationRequest;
import com.thinkerscave.platform.dto.response.ProvisioningJobResponse;
import com.thinkerscave.platform.dto.response.ProvisioningResultResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.CustomerContact;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.ContactType;
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
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.platform.service.ProvisionService;
import com.thinkerscave.security.service.OutboundMessageService;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvisionServiceImpl implements ProvisionService {

    private static final String ROLE_ORG_ADMIN_CODE = "ROLE_ADMIN";
    private static final String ROLE_ORG_OWNER_CODE = "ROLE_OWNER";
    private static final List<String> REQUIRED_TENANT_TABLES = List.of(
            "users", "roles", "user_roles", "tenant_registry", "organizations"
    );

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
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OutboundMessageService outboundMessageService;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final SubscriptionPlanFeatureRepository subscriptionPlanFeatureRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.tenancy.platform-schema:thinkerscave_dev}")
    private String platformSchema;

    @Value("${spring.flyway.enabled:false}")
    private boolean flywayEnabled;

    @Value("${app.platform.login-url:http://localhost:4200/auth/login}")
    private String platformLoginUrl;

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
                throw new BadRequestException("Customer email and customer name are required when creating a new customer");
            }
            customer = customerRepository.findByBusinessEmail(request.getCustomerEmail())
                    .orElseGet(() -> {
                        String code = codeGeneratorService.generate(CodeType.CUSTOMER);
                        String name = request.getCustomerLegalName();
                        String display = request.getCustomerDisplayName() != null
                                ? request.getCustomerDisplayName()
                                : name;
                        Customer created = Customer.builder()
                                .customerCode(code)
                                .customerName(display != null ? display : name)
                                .businessEmail(request.getCustomerEmail().trim().toLowerCase())
                                .mobileNumber(request.getCustomerMobile() != null ? request.getCustomerMobile() : "0000000000")
                                .status(com.thinkerscave.platform.enums.CustomerStatus.ACTIVE)
                                .active(true)
                                .build();
                        CustomerContact primary = CustomerContact.builder()
                                .contactCode(codeGeneratorService.generate(CodeType.CONTACT))
                                .contactType(ContactType.PRIMARY)
                                .fullName(name != null ? name : "Owner")
                                .email(request.getCustomerEmail().trim().toLowerCase())
                                .mobileNumber(request.getCustomerMobile() != null ? request.getCustomerMobile() : "0000000000")
                                .active(true)
                                .build();
                        created.addContact(primary);
                        return customerRepository.save(created);
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
                .status(OrganizationStatus.PENDING)
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

        String provisionedSchemaName = null;
        try {
            // ── Step 7: Create TenantRegistry ────────────────────────────────
            String subDomainSlug = normalizeSubdomain(
                    request.getTenantSubdomain() != null && !request.getTenantSubdomain().isBlank()
                            ? request.getTenantSubdomain()
                            : request.getOrganizationName()
            );
            String tenantId = normalizeTenantIdentifier(subDomainSlug);
            String schemaName = tenantId;
            provisionedSchemaName = schemaName;
            String tenantDomain = subDomainSlug + ".thinkerscave.app";

            TenantRegistry tenant = TenantRegistry.builder()
                    .tenantIdentifier(tenantId)
                    .organization(org)
                    .schemaName(schemaName)
                    .provisionStatus(ProvisionStatus.IN_PROGRESS)
                    .tenantDomain(tenantDomain)
                    .maintenanceMode(false)
                    .active(false)
                    .build();
            tenant = tenantRepository.save(tenant);
            log.info("TenantRegistry created: {} -> schema: {}", tenantId, schemaName);

                // ── Step 8: Schema provisioning + validation ───────────────────────
                provisionTenantSchema(schemaName);
                verifyTenantSchemaReadiness(schemaName);
                tenant.setProvisionStatus(ProvisionStatus.COMPLETED);
                tenant.setMigrationVersion(flywayEnabled ? "flyway" : "bootstrap-copy");
                tenant.setLastMigrationAt(LocalDateTime.now());
                tenant.setActive(true);
                tenantRepository.save(tenant);
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

            // ── Step 15: Create admin user ────────────────────────────────────
            // Organization Admin is an "internal" tenant user: it must be created
            // directly inside the organization's own schema, never duplicated into
            // the public/platform schema (only SUPER_ADMIN and Customer/Organization
            // Owner accounts live in public — see mapCustomerOwnerToOrganization below).
            //
            // Uses raw JDBC (same fully-qualified "schema"."table" pattern as
            // copyPlatformRows) instead of JPA/Hibernate for two reasons found during
            // testing:
            // (1) Hibernate binds one schema to a session for its whole transaction
            //     lifetime — flipping TenantContext mid-transaction on this method's
            //     own (ambient) session is a no-op and non-deterministically left the
            //     admin user in the public schema instead.
            // (2) A @Transactional(REQUIRES_NEW) helper (a separate DB connection)
            //     cannot see this transaction's own not-yet-committed tenant schema/
            //     tables (Postgres MVCC visibility), causing "relation ... does not
            //     exist" errors — provisionTenantSchema() (Step 8) creates the schema
            //     earlier in this SAME still-open transaction, so it isn't visible to
            //     any other connection until this method commits.
            // Raw JDBC on the SAME connection/transaction sidesteps both problems.
            //
            // The tenant schema's "roles" table is normally populated later, inside
            // bootstrapTenantWorkspace() — but the admin user creation below needs to
            // resolve ROLE_ADMIN by code against the tenant schema *first*, so copy just
            // that one reference table here (idempotent; bootstrapTenantWorkspace's own
            // copy of it afterwards is a harmless no-op via ON CONFLICT DO NOTHING).
            copyPlatformRows(schemaName, resolvePlatformSourceSchema(), "roles", null);

            String adminEmail = request.getAdminEmail().trim().toLowerCase();
            String adminLastName = request.getAdminLastName() != null ? request.getAdminLastName() : "";
            String adminDisplayName = request.getAdminFirstName()
                    + (!adminLastName.isBlank() ? " " + adminLastName : "");
            String adminUserCode = codeGeneratorService.generate(CodeType.USER);
            String adminTemporaryPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

            Long adminRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM \"" + schemaName + "\".\"roles\" WHERE role_code = ?",
                    Long.class, ROLE_ORG_ADMIN_CODE);

            Long adminUserId = jdbcTemplate.queryForObject(
                    "INSERT INTO \"" + schemaName + "\".\"users\" "
                            + "(user_code, email, username, mobile_number, password, first_name, last_name, "
                            + "display_name, organization_id, status, first_time_login, email_verified, "
                            + "mobile_verified, account_locked, failed_login_attempts, version) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0) RETURNING id",
                    Long.class,
                    adminUserCode, adminEmail, adminEmail, request.getAdminMobile(),
                    passwordEncoder.encode(adminTemporaryPassword),
                    request.getAdminFirstName(), adminLastName, adminDisplayName,
                    org.getId(), UserStatus.ACTIVE.name(), true, false, false, false, 0);

            jdbcTemplate.update(
                    "INSERT INTO \"" + schemaName + "\".\"user_roles\" "
                            + "(user_id, role_id, primary_role, active, version) VALUES (?,?,?,?,0)",
                    adminUserId, adminRoleId, true, true);

            User adminUser = User.builder()
                    .id(adminUserId)
                    .userCode(adminUserCode)
                    .email(adminEmail)
                    .username(adminEmail)
                    .mobileNumber(request.getAdminMobile())
                    .firstName(request.getAdminFirstName())
                    .lastName(adminLastName)
                    .displayName(adminDisplayName)
                    .organizationId(org.getId())
                    .status(UserStatus.ACTIVE)
                    .firstTimeLogin(true)
                    .build();
            log.info("Admin user created directly in tenant schema {} for org {}: {}", schemaName, orgCode, adminUser.getEmail());
            updateJobProgress(job, "ADMIN_USER_CREATED", 90);

            // ── Step 16: Map customer owner to this organization ──────────────
            // Organization Owner remains an "external"/platform identity (it can own
            // and switch across multiple organizations), so it stays in public only —
            // it is intentionally NOT duplicated into the tenant schema.
            mapCustomerOwnerToOrganization(customer, org);
            updateJobProgress(job, "CUSTOMER_OWNER_MAPPED", 94);

            // Seed tenant catalog with auth/workspace rows so institution login
            // can resolve tenant_registry + admin/owner users in the tenant DB.
            bootstrapTenantWorkspace(schemaName, tenant, org, customer, adminUser);

            // Grant the organization access to the menus its subscription plan
            // entitles it to, and full permissions on those menus for the Owner
            // and Admin roles — this is what makes the sidebar non-empty on first login.
            seedTenantMenuEntitlementsAndPermissions(schemaName, org, plan, request.getDisabledFeatureIds());

            // Keep onboarding pending for first-login checklist.
            org.setOnboardingCompleted(false);
            org.setStatus(OrganizationStatus.ACTIVE);
            organizationRepository.save(org);

            // ── Step 17: Complete Job ─────────────────────────────────────────
            LocalDateTime completedAt = LocalDateTime.now();
            job.setStatus(ProvisionJobStatus.COMPLETED);
            job.setCurrentStep("COMPLETED");
            job.setProgressPercentage(100);
            job.setCompletedAt(completedAt);
            job.setDurationSeconds(java.time.Duration.between(start, completedAt).getSeconds());
            job.setTenantRegistry(tenant);
            jobRepository.save(job);

            sendProvisioningEmails(customer, org, tenantDomain, adminUser, adminTemporaryPassword);

            log.info("Provisioning completed: orgCode={}, tenantId={}, jobCode={}", orgCode, tenantId, jobCode);

            // ── Step 18: Return result ────────────────────────────────────────
            log.info("Provisioning credentials: orgCode={} adminUsername={} temporaryPassword={}",
                    orgCode, adminUser.getUsername(), adminTemporaryPassword);
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
                    .adminEmail(adminUser.getEmail())
                    .adminUsername(adminUser.getUsername())
                    .temporaryPassword(adminTemporaryPassword)
                    .defaultDomain(tenantDomain)
                    .message("Organization provisioned successfully.")
                    .build();

        } catch (Exception ex) {
            log.error("Provisioning failed for org: {}, error: {}", orgCode, ex.getMessage(), ex);
            cleanupTenantSchema(provisionedSchemaName);
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

    private void provisionTenantSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new BadRequestException("Tenant schema name is required for provisioning");
        }
        log.info("Provisioning tenant schema: {}", schemaName);

        boolean postgres = isPostgres();
        if (postgres) {
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
        } else {
            jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS `" + schemaName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        String sourceSchema = resolvePlatformSourceSchema();
        List<String> platformTables = fetchPlatformTables(sourceSchema);
        for (String table : platformTables) {
            if (postgres) {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS \"" + schemaName + "\".\"" + table + "\" (LIKE \""
                        + sourceSchema + "\".\"" + table + "\" INCLUDING ALL)");
            } else {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schemaName + "`.`" + table + "` LIKE `" + sourceSchema + "`.`" + table + "`");
            }
        }
        log.info("Tenant schema initialized: {} with {} tables", schemaName, platformTables.size());
    }

    private void verifyTenantSchemaReadiness(String schemaName) {
        List<String> missingTables = new ArrayList<>();
        boolean postgres = isPostgres();
        try {
            for (String table : REQUIRED_TENANT_TABLES) {
                if (postgres) {
                    String regClass = jdbcTemplate.queryForObject(
                            "SELECT to_regclass(?)",
                            String.class,
                            schemaName + "." + table
                    );
                    if (regClass == null) {
                        missingTables.add(table);
                    }
                } else {
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
                            Integer.class,
                            schemaName,
                            table
                    );
                    if (count == null || count == 0) {
                        missingTables.add(table);
                    }
                }
            }
        } catch (Exception ex) {
            throw new BadRequestException("Tenant schema validation failed: " + ex.getMessage());
        }

        if (!missingTables.isEmpty()) {
            throw new BadRequestException("Tenant schema is missing required tables: " + String.join(", ", missingTables));
        }
        log.info("Tenant schema validation completed: {}", schemaName);
    }

    private void cleanupTenantSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return;
        }
        try {
            if (isPostgres()) {
                jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            } else {
                jdbcTemplate.execute("DROP DATABASE IF EXISTS `" + schemaName + "`");
            }
            log.warn("Provisioning rollback cleanup executed for tenant schema={}", schemaName);
        } catch (Exception cleanupEx) {
            log.error("Failed to cleanup tenant schema {} after provisioning failure: {}", schemaName, cleanupEx.getMessage(), cleanupEx);
        }
    }

    private boolean isPostgres() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("postgresql");
        } catch (Exception ex) {
            return false;
        }
    }

    private String resolvePlatformSourceSchema() {
        List<String> configuredTables = fetchPlatformTables(platformSchema);
        if (!configuredTables.isEmpty()) {
            return platformSchema;
        }

        if (isPostgres()) {
            try {
                String currentSchema = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
                if (currentSchema != null && !currentSchema.isBlank()) {
                    List<String> currentSchemaTables = fetchPlatformTables(currentSchema);
                    if (!currentSchemaTables.isEmpty()) {
                        log.warn("Configured platform schema '{}' has no tables; using detected schema '{}' for provisioning clone", platformSchema, currentSchema);
                        return currentSchema;
                    }
                }
            } catch (Exception ignored) {
                // Fall through to throw a clear schema/table error below.
            }
        }

        throw new BadRequestException("No platform tables found in schema: " + platformSchema);
    }

    private List<String> fetchPlatformTables(String schemaName) {
        List<String> tables = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            try (Statement st = connection.createStatement();
                 ResultSet rs = st.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema='" + schemaName + "' AND table_type='BASE TABLE'")) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        } catch (Exception ex) {
            throw new BadRequestException("Unable to inspect platform schema tables: " + ex.getMessage());
        }
        return tables;
    }

    private void mapCustomerOwnerToOrganization(Customer customer, Organization organization) {
        if (customer.getOwnerUserId() == null) {
            return;
        }
        userRepository.findById(customer.getOwnerUserId()).ifPresent(owner -> {
            if (owner.getOrganizationId() == null || owner.getOrganizationId() <= 0) {
                owner.setOrganizationId(organization.getId());
                userRepository.save(owner);
            }
        });
    }

    /**
     * Copies the minimum platform reference rows required for tenant-scoped
     * lookups and first-load dashboard into the newly created tenant catalog.
     * Table structures alone are not enough — institution login runs against the tenant DB.
     *
     * Deliberately does NOT copy "users"/"user_roles" for the admin or owner:
     * the Organization Admin is created directly inside this schema (see Step 15
     * in provision()), and the Organization Owner is an "external"/platform
     * identity that must remain exclusively in the public schema. Duplicating
     * either into this schema previously caused two independent credential rows
     * to drift out of sync (different password hashes) after any password change.
     */
    private void bootstrapTenantWorkspace(
            String schemaName,
            TenantRegistry tenant,
            Organization organization,
            Customer customer,
            User adminUser) {
        if (schemaName == null || schemaName.isBlank()) {
            return;
        }

        String sourceSchema = resolvePlatformSourceSchema();

        try {
            copyPlatformRows(schemaName, sourceSchema, "roles", null);
            copyPlatformRows(schemaName, sourceSchema, "customers", "id = " + customer.getId());
            copyPlatformRows(schemaName, sourceSchema, "organizations", "id = " + organization.getId());
            copyPlatformRows(schemaName, sourceSchema, "tenant_registry", "id = " + tenant.getId());
            copyPlatformRows(schemaName, sourceSchema, "organization_domains", "organization_id = " + organization.getId());
            copyPlatformRows(schemaName, sourceSchema, "organization_configurations", "organization_id = " + organization.getId());
            copyPlatformRows(schemaName, sourceSchema, "organization_subscriptions", "organization_id = " + organization.getId());
            // PLATFORM-scope menus (Tenant Management) are Super Admin-only and must
            // never be duplicated into a tenant schema; only CORE/SUBSCRIPTION menus
            // are copied here as the full org-facing catalog.
            copyPlatformRows(schemaName, sourceSchema, "menus", "menu_scope <> 'PLATFORM'");

            log.info("Tenant workspace bootstrap completed for schema={}", schemaName);
        } catch (Exception ex) {
            throw new BadRequestException("Failed to bootstrap tenant workspace data: " + ex.getMessage());
        }
    }

    private void copyPlatformRows(String tenantSchema, String sourceSchema, String table, String whereClause) {
        String sql;
        if (isPostgres()) {
            sql = "INSERT INTO \"" + tenantSchema + "\".\"" + table + "\" "
                    + "SELECT * FROM \"" + sourceSchema + "\".\"" + table + "\""
                    + (whereClause != null && !whereClause.isBlank() ? " WHERE " + whereClause : "")
                    + " ON CONFLICT DO NOTHING";
        } else {
            sql = "INSERT IGNORE INTO `" + tenantSchema + "`.`" + table + "` "
                    + "SELECT * FROM `" + sourceSchema + "`.`" + table + "`"
                    + (whereClause != null && !whereClause.isBlank() ? " WHERE " + whereClause : "");
        }
        jdbcTemplate.execute(sql);
    }

    /**
     * Computes the menus this organization is entitled to (CORE menus always,
     * plus SUBSCRIPTION menus matching the plan's enabled features minus any
     * explicitly disabled feature overrides), records them as the organization's
     * "available modules" cache, and grants full permissions on the entitled
     * leaf (PAGE) menus to the Owner and Admin roles only. No other role gets
     * automatic permissions — this matches the approved Workflow 04 architecture.
     * <p>
     * Uses raw JDBC against fully-qualified "{schema}"."table" identifiers rather
     * than JPA/TenantContext switching, because the enclosing {@code provision()}
     * transaction already holds one Hibernate session bound to the schema active
     * when it began; flipping {@code TenantContext} mid-transaction does not
     * reliably re-route JPA repository calls to the tenant schema (see
     * {@link #copyPlatformRows} for the same established pattern).
     */
    private void seedTenantMenuEntitlementsAndPermissions(
            String schemaName,
            Organization organization,
            SubscriptionPlan plan,
            List<Long> disabledFeatureIds) {
        if (schemaName == null || schemaName.isBlank()) {
            return;
        }

        Set<Long> enabledFeatureIds = subscriptionPlanFeatureRepository
                .findBySubscriptionPlan_IdAndEnabledTrueAndActiveTrue(plan.getId())
                .stream()
                .map(spf -> spf.getFeature().getId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (disabledFeatureIds != null) {
            enabledFeatureIds.removeAll(disabledFeatureIds);
        }
        String featureIdList = enabledFeatureIds.isEmpty()
                ? "-1"
                : enabledFeatureIds.stream().map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","));

        // Resolve top-level CORE menus + top-level SUBSCRIPTION menus matching the
        // plan's enabled features, then expand to all descendants — all in one query.
        String entitledMenusCte =
                "WITH RECURSIVE entitled AS (" +
                        "SELECT id, menu_type FROM \"" + schemaName + "\".\"menus\" " +
                        "WHERE parent_menu_id IS NULL AND active = true " +
                        "AND (menu_scope = 'CORE' OR (menu_scope = 'SUBSCRIPTION' AND feature_id IN (" + featureIdList + "))) " +
                        "UNION ALL " +
                        "SELECT m.id, m.menu_type FROM \"" + schemaName + "\".\"menus\" m " +
                        "INNER JOIN entitled e ON m.parent_menu_id = e.id " +
                        "WHERE m.active = true" +
                        ") ";

        List<Long> entitledMenuIds = jdbcTemplate.query(entitledMenusCte + "SELECT id FROM entitled",
                (rs, rowNum) -> rs.getLong("id"));

        if (entitledMenuIds.isEmpty()) {
            log.warn("No entitled menus resolved for organization {} on plan {} (schema={}); sidebar will be empty",
                    organization.getOrganizationCode(), plan.getPlanCode(), schemaName);
            return;
        }

        String menuIdList = entitledMenuIds.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));

        // "Available modules" cache — every entitled menu (MODULE and PAGE alike).
        jdbcTemplate.execute(
                "INSERT INTO \"" + schemaName + "\".\"organization_modules\" " +
                        "(organization_id, menu_id, enabled, created_on, version) " +
                        "SELECT " + organization.getId() + ", id, true, now(), 0 " +
                        "FROM \"" + schemaName + "\".\"menus\" WHERE id IN (" + menuIdList + ") " +
                        "ON CONFLICT (organization_id, menu_id) DO NOTHING");

        // Full access on entitled leaf (PAGE) menus for Owner and Admin roles only.
        jdbcTemplate.execute(
                "INSERT INTO \"" + schemaName + "\".\"role_permissions\" " +
                        "(organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_on, version) " +
                        "SELECT " + organization.getId() + ", r.id, m.id, true, true, true, now(), 0 " +
                        "FROM \"" + schemaName + "\".\"roles\" r " +
                        "CROSS JOIN \"" + schemaName + "\".\"menus\" m " +
                        "WHERE r.role_code IN ('" + ROLE_ORG_OWNER_CODE + "', '" + ROLE_ORG_ADMIN_CODE + "') " +
                        "AND m.id IN (" + menuIdList + ") AND m.menu_type = 'PAGE' " +
                        "ON CONFLICT (organization_id, role_id, menu_id) DO NOTHING");

        log.info("Seeded {} entitled menus and default Owner/Admin permissions for organization {} (schema={})",
                entitledMenuIds.size(), organization.getOrganizationCode(), schemaName);
    }

    private void sendProvisioningEmails(
            Customer customer,
            Organization organization,
            String workspaceDomain,
            User adminUser,
            String adminTemporaryPassword) {
        String workspaceUrl = "https://" + workspaceDomain + "/auth/login";

        CustomerContact primaryContact = customer.getContacts().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()) && c.getContactType() == ContactType.PRIMARY)
                .findFirst()
                .orElse(null);

        if (primaryContact != null && primaryContact.getEmail() != null && !primaryContact.getEmail().isBlank()) {
            try {
                String ownerMobile = firstNonBlank(
                        primaryContact.getMobileNumber(),
                        customer.getMobileNumber(),
                        organization.getMobileNumber());
                outboundMessageService.sendOrganizationOwnerReady(
                        primaryContact.getEmail(),
                        ownerMobile,
                        primaryContact.getFullName(),
                        organization.getOrganizationName(),
                        workspaceUrl);
                log.info("Provisioning owner notified for customer={} org={}",
                        customer.getCustomerCode(), organization.getOrganizationCode());
            } catch (Exception ex) {
                log.error("Failed to notify customer owner for org {}: {}",
                        organization.getOrganizationCode(), ex.getMessage(), ex);
            }
        }

        try {
            String adminMobile = firstNonBlank(adminUser.getMobileNumber(), organization.getMobileNumber());
            outboundMessageService.sendOrganizationAdminWelcome(
                    adminUser.getEmail(),
                    adminMobile,
                    adminUser.getDisplayName(),
                    organization.getOrganizationName(),
                    workspaceUrl,
                    adminUser.getUsername(),
                    adminTemporaryPassword);
            log.info("Provisioning admin notified for org={} admin={}",
                    organization.getOrganizationCode(), adminUser.getEmail());
        } catch (Exception ex) {
            log.error("Failed to notify organization admin for org {}: {}",
                    organization.getOrganizationCode(), ex.getMessage(), ex);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

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
        BigDecimal discount = switch (promotion.getDiscountType()) {
            case PERCENTAGE -> price.multiply(promotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
            case FLAT_AMOUNT -> promotion.getDiscountValue().min(price);
        };
        if (promotion.getMaximumDiscount() != null && promotion.getMaximumDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discount.min(promotion.getMaximumDiscount());
        }
        return discount.min(price);
    }

    private String normalizeSubdomain(String value) {
        if (value == null || value.isBlank()) {
            return "tenant";
        }
        return value.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private String normalizeTenantIdentifier(String subdomain) {
        String normalized = normalizeSubdomain(subdomain).replace('-', '_');
        if (normalized.startsWith("tenant_")) {
            return normalized;
        }
        return "tenant_" + normalized;
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
