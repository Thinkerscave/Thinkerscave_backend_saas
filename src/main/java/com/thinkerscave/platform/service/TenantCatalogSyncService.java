package com.thinkerscave.platform.service;

import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.Feature;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.repository.SubscriptionFeatureOverrideRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.access.repository.MenuRepository;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.platform.repository.FeatureRepository;
import com.thinkerscave.platform.dto.response.CatalogSyncExecutionResponse;
import com.thinkerscave.platform.dto.response.CatalogSyncStatusResponse;
import com.thinkerscave.platform.enums.OperationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.function.Supplier;

/**
 * Pushes platform catalog changes (menus / roles) into already-provisioned tenant
 * schemas. Catalog definitions, entitlements and permissions are reconciled independently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCatalogSyncService {

    private static final Set<String> CANONICAL_ROLE_CODES = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_OWNER", "ROLE_ADMIN", "ROLE_STAFF", "ROLE_STUDENT", "ROLE_PARENT");

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanFeatureRepository planFeatureRepository;
    private final SubscriptionFeatureOverrideRepository overrideRepository;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final FeatureRepository featureRepository;

    @Value("${app.tenancy.platform-schema:thinkerscave_dev}")
    private String platformSchema;

    public void syncFeature(Feature feature) {
        if (feature == null || feature.getId() == null) {
            return;
        }
        Long featureId = feature.getId();
        afterCommitOrNow(() -> featureRepository.findById(featureId).ifPresent(this::syncFeatureNow));
    }

    private void syncFeatureNow(Feature feature) {
        String source = quoteIdent(resolveSourceSchema());
        Long version = inNewTransaction(
                () -> recordCatalogChange("UPSERT", "FEATURE", feature.getFeatureCode()));
        for (TenantRegistry tenant : completedTenants()) {
            Long execution = inNewTransaction(() -> {
                Long id = startSync(version, tenant.getId());
                auditCatalog("CATALOG_SYNC_STARTED", tenant, id, version, "RUNNING", null);
                return id;
            });
            try {
                inNewTransaction(() -> {
                    upsertFeatureWithAncestors(source, quoteIdent(tenant.getSchemaName()), feature);
                    completeSync(execution, tenant, version, null);
                    auditCatalog("CATALOG_SYNC_COMPLETED", tenant, execution, version, "SUCCESS", null);
                });
            } catch (Exception ex) {
                inNewTransaction(() -> {
                    failLatestSync(version, tenant.getId(), ex);
                    auditCatalog("CATALOG_SYNC_FAILED", tenant, execution, version, "FAILED", ex.getMessage());
                });
                log.error("Failed to sync feature {} to tenant {}", feature.getFeatureCode(),
                        tenant.getTenantIdentifier(), ex);
            }
        }
    }

    public void syncMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            return;
        }
        Long menuId = menu.getId();
        afterCommitOrNow(() -> menuRepository.findById(menuId).ifPresent(this::syncMenuNow));
    }

    private void syncMenuNow(Menu menu) {
        if (menu.getMenuScope() == MenuScope.PLATFORM) {
            return;
        }
        String source = quoteIdent(resolveSourceSchema());
        Long catalogVersion = inNewTransaction(
                () -> recordCatalogChange("UPSERT", "MENU", menu.getMenuCode()));
        for (TenantRegistry tenant : completedTenants()) {
            String schema = quoteIdent(tenant.getSchemaName());
            Long executionId = inNewTransaction(() -> {
                Long id = startSync(catalogVersion, tenant.getId());
                auditCatalog("CATALOG_SYNC_STARTED", tenant, id, catalogVersion, "RUNNING", null);
                return id;
            });
            try {
                inNewTransaction(() -> {
                    upsertMenuWithAncestors(source, schema, menu);
                    completeSync(executionId, tenant, catalogVersion, null);
                    auditCatalog("CATALOG_SYNC_COMPLETED", tenant, executionId, catalogVersion, "SUCCESS", null);
                });
            } catch (Exception ex) {
                log.error("Failed to sync menu {} to tenant {}: {}",
                        menu.getMenuCode(), tenant.getTenantIdentifier(), ex.getMessage());
                inNewTransaction(() -> {
                    failLatestSync(catalogVersion, tenant.getId(), ex);
                    auditCatalog("CATALOG_SYNC_FAILED", tenant, executionId, catalogVersion, "FAILED", ex.getMessage());
                });
            }
        }
    }

    public void syncRole(Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        Long roleId = role.getId();
        afterCommitOrNow(() -> roleRepository.findById(roleId).ifPresent(this::syncRoleNow));
    }

    private void syncRoleNow(Role role) {
        if (!CANONICAL_ROLE_CODES.contains(role.getRoleCode())) {
            log.info("Skipping non-canonical tenant role {}", role.getRoleCode());
            return;
        }
        String source = quoteIdent(resolveSourceSchema());
        Long catalogVersion = inNewTransaction(
                () -> recordCatalogChange("UPSERT", "ROLE", role.getRoleCode()));
        for (TenantRegistry tenant : completedTenants()) {
            String schema = quoteIdent(tenant.getSchemaName());
            Long executionId = inNewTransaction(() -> {
                Long id = startSync(catalogVersion, tenant.getId());
                auditCatalog("CATALOG_SYNC_STARTED", tenant, id, catalogVersion, "RUNNING", null);
                return id;
            });
            try {
                inNewTransaction(() -> {
                    upsertRole(source, schema, role.getId());
                    completeSync(executionId, tenant, catalogVersion, null);
                    auditCatalog("CATALOG_SYNC_COMPLETED", tenant, executionId, catalogVersion, "SUCCESS", null);
                });
            } catch (Exception ex) {
                log.error("Failed to sync role {} to tenant {}: {}",
                        role.getRoleCode(), tenant.getTenantIdentifier(), ex.getMessage(), ex);
                inNewTransaction(() -> {
                    failLatestSync(catalogVersion, tenant.getId(), ex);
                    auditCatalog("CATALOG_SYNC_FAILED", tenant, executionId, catalogVersion, "FAILED", ex.getMessage());
                });
            }
        }
    }

    public void removeMenu(Long menuId) {
        if (menuId == null) {
            return;
        }
        afterCommitOrNow(() -> removeMenuNow(menuId));
    }

    private void removeMenuNow(Long menuId) {
        Long catalogVersion = inNewTransaction(
                () -> recordCatalogChange("REMOVE", "MENU", menuId.toString()));
        String sourceSchema = resolveSourceSchema();
        for (TenantRegistry tenant : completedTenants()) {
            if (tenant.getSchemaName() != null && tenant.getSchemaName().equalsIgnoreCase(sourceSchema)) {
                continue;
            }
            String schema = quoteIdent(tenant.getSchemaName());
            Long executionId = inNewTransaction(() -> {
                Long id = startSync(catalogVersion, tenant.getId());
                auditCatalog("CATALOG_SYNC_STARTED", tenant, id, catalogVersion, "RUNNING", null);
                return id;
            });
            try {
                inNewTransaction(() -> {
                    deleteMenuFromSchema(schema, menuId);
                    completeSync(executionId, tenant, catalogVersion, null);
                    auditCatalog("CATALOG_SYNC_COMPLETED", tenant, executionId, catalogVersion, "SUCCESS", null);
                });
            } catch (Exception ex) {
                log.error("Failed to remove menu {} from tenant {}: {}",
                        menuId, tenant.getTenantIdentifier(), ex.getMessage(), ex);
                inNewTransaction(() -> {
                    failLatestSync(catalogVersion, tenant.getId(), ex);
                    auditCatalog("CATALOG_SYNC_FAILED", tenant, executionId, catalogVersion, "FAILED", ex.getMessage());
                });
            }
        }
    }

    public void reseedOrganization(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        Long organizationId = organization.getId();
        afterCommitOrNow(() -> {
            Organization managed = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new IllegalStateException("Organization not found: " + organizationId));
            inNewTransaction(() -> reseedOrganizationNow(managed));
        });
    }

    private void reseedOrganizationNow(Organization managedOrganization) {
        tenantRegistryRepository.findByOrganization_Id(managedOrganization.getId()).ifPresent(tenant -> {
            if (!Boolean.TRUE.equals(tenant.getActive())
                    || tenant.getProvisionStatus() != ProvisionStatus.COMPLETED
                    || !StringUtils.hasText(tenant.getSchemaName())) {
                return;
            }
            try {
                String source = quoteIdent(resolveSourceSchema());
                String schema = quoteIdent(tenant.getSchemaName());
                upsertFeaturesByNaturalKey(source, schema, "true");
                copyOrgFacingMenus(source, schema);
                reseedEntitlements(schema, managedOrganization);
            } catch (Exception ex) {
                log.error("Failed to reseed entitlements for org {}: {}",
                        managedOrganization.getOrganizationCode(), ex.getMessage(), ex);
                throw new IllegalStateException("Tenant catalog reconciliation failed", ex);
            }
        });
    }

    private void deleteMenuFromSchema(String schema, Long menuId) {
        jdbcTemplate.update("UPDATE " + schema + ".\"menus\" SET active=false, updated_on=now() WHERE id = ?", menuId);
    }

    private Set<String> entitledFeatureCodes(Organization organization) {
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_IdAndActiveTrue(organization.getId())
                .orElse(null);
        if (subscription == null || subscription.getSubscriptionPlan() == null) {
            return Set.of();
        }
        Set<String> enabled = planFeatureRepository
                .findBySubscriptionPlan_IdAndEnabledTrueAndActiveTrue(subscription.getSubscriptionPlan().getId())
                .stream()
                .map(spf -> spf.getFeature().getFeatureCode())
                .collect(Collectors.toCollection(HashSet::new));
        List<SubscriptionFeatureOverride> overrides = overrideRepository
                .findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(subscription.getId());
        for (SubscriptionFeatureOverride override : overrides) {
            String featureCode = override.getFeature().getFeatureCode();
            if (Boolean.TRUE.equals(override.getEnabled())) {
                enabled.add(featureCode);
            } else {
                enabled.remove(featureCode);
            }
        }
        return enabled;
    }

    /**
     * Reconciles only explicit role permissions. Catalog definitions and
     * subscription entitlements are intentionally untouched.
     */
    public void syncRolePermissions(Long roleId, Long organizationId) {
        if (roleId == null || organizationId == null) {
            return;
        }
        afterCommitOrNow(() -> syncRolePermissionsNow(roleId, organizationId));
    }

    private void syncRolePermissionsNow(Long roleId, Long organizationId) {
        TenantRegistry tenant = tenantRegistryRepository.findByOrganization_Id(organizationId).orElse(null);
        if (tenant == null || !completedTenants().stream().anyMatch(t -> t.getId().equals(tenant.getId()))) {
            return;
        }
        String source = quoteIdent(resolveSourceSchema());
        String schema = quoteIdent(tenant.getSchemaName());
        Long version = inNewTransaction(
                () -> recordCatalogChange(
                        "RECONCILE", "ROLE_PERMISSION", roleId + ":" + organizationId, tenant.getId()));
        Long executionId = inNewTransaction(() -> {
            Long id = startSync(version, tenant.getId());
            auditCatalog("CATALOG_SYNC_STARTED", tenant, id, version, "RUNNING", null);
            return id;
        });
        try {
            inNewTransaction(() -> {
                reconcileRolePermissions(source, schema, roleId, organizationId);
                Organization organization = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Organization not found: " + organizationId));
                // Refresh the materialized entitlement cache from the existing
                // subscription. This never grants a feature outside the plan.
                upsertFeaturesByNaturalKey(source, schema, "true");
                copyOrgFacingMenus(source, schema);
                reseedEntitlements(schema, organization);
                completeSync(executionId, tenant, version, null);
                auditCatalog("CATALOG_SYNC_COMPLETED", tenant, executionId, version, "SUCCESS", null);
            });
        } catch (Exception ex) {
            inNewTransaction(() -> {
                failLatestSync(version, tenant.getId(), ex);
                auditCatalog("CATALOG_SYNC_FAILED", tenant, executionId, version, "FAILED", ex.getMessage());
            });
            throw ex;
        }
    }

    private void reconcileRolePermissions(String source, String schema, Long roleId, Long organizationId) {
        jdbcTemplate.update("DELETE FROM " + schema + ".\"role_permissions\" rp USING "
                        + schema + ".\"roles\" r WHERE rp.role_id=r.id AND rp.organization_id=? "
                        + "AND r.role_code=(SELECT role_code FROM " + source + ".\"roles\" WHERE id=?)",
                organizationId, roleId);
        jdbcTemplate.update("""
                INSERT INTO %s."role_permissions"
                    (organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_on, version)
                SELECT rp.organization_id, tr.id, tm.id, rp.can_view, rp.can_manage, rp.can_approve, now(), 0
                FROM %s."role_permissions" rp
                JOIN %s."roles" sr ON sr.id=rp.role_id
                JOIN %s."menus" sm ON sm.id=rp.menu_id
                JOIN %s."roles" tr ON tr.role_code=sr.role_code
                JOIN %s."menus" tm ON tm.menu_code=sm.menu_code
                WHERE rp.role_id=? AND rp.organization_id=?
                ON CONFLICT (organization_id, role_id, menu_id) DO UPDATE SET
                    can_view=EXCLUDED.can_view, can_manage=EXCLUDED.can_manage,
                    can_approve=EXCLUDED.can_approve, updated_on=now()
                """.formatted(schema, source, source, source, schema, schema), roleId, organizationId);
    }

    @Transactional
    public void retry(Long executionId) {
        var row = jdbcTemplate.queryForMap("""
                SELECT c.entity_type, c.entity_key
                FROM catalog_sync_executions e
                JOIN catalog_versions c ON c.id=e.catalog_version_id
                WHERE e.id=? AND e.status='FAILED'
                """, executionId);
        String type = String.valueOf(row.get("entity_type"));
        String key = String.valueOf(row.get("entity_key"));
        if ("MENU".equals(type)) {
            menuRepository.findByMenuCode(key).ifPresent(this::syncMenu);
        } else if ("FEATURE".equals(type)) {
            featureRepository.findByFeatureCode(key).ifPresent(this::syncFeature);
        } else if ("ROLE".equals(type)) {
            roleRepository.findByRoleCode(key).ifPresent(this::syncRole);
        } else if ("ROLE_PERMISSION".equals(type)) {
            String[] parts = key.split(":");
            syncRolePermissions(Long.valueOf(parts[0]), Long.valueOf(parts[1]));
        }
    }

    public CatalogSyncExecutionResponse retryLatestForTenant(Long tenantId) {
        var failed = jdbcTemplate.queryForMap("""
                SELECT e.catalog_version_id, e.attempt, c.entity_type, c.entity_key, c.change_type
                FROM catalog_sync_executions e
                JOIN catalog_versions c ON c.id=e.catalog_version_id
                WHERE e.tenant_registry_id=? AND e.status='FAILED'
                ORDER BY e.created_on DESC LIMIT 1
                """, tenantId);
        TenantRegistry tenant = tenantRegistryRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Long versionId = ((Number) failed.get("catalog_version_id")).longValue();
        int attempt = ((Number) failed.get("attempt")).intValue() + 1;
        Long executionId = inNewTransaction(() -> startSync(versionId, tenantId, attempt));
        String source = quoteIdent(resolveSourceSchema());
        String schema = quoteIdent(tenant.getSchemaName());
        String type = String.valueOf(failed.get("entity_type"));
        String key = String.valueOf(failed.get("entity_key"));
        String changeType = String.valueOf(failed.get("change_type"));
        try {
            inNewTransaction(() -> {
                if ("REMOVE".equals(changeType)) {
                    deleteMenuFromSchema(schema, Long.valueOf(key));
                } else if ("MENU".equals(type)) {
                    Menu menu = menuRepository.findByMenuCode(key)
                            .orElseThrow(() -> new IllegalArgumentException("Menu no longer exists"));
                    upsertMenuWithAncestors(source, schema, menu);
                } else if ("FEATURE".equals(type)) {
                    Feature feature = featureRepository.findByFeatureCode(key)
                            .orElseThrow(() -> new IllegalArgumentException("Feature no longer exists"));
                    upsertFeatureWithAncestors(source, schema, feature);
                } else if ("ROLE".equals(type)) {
                    Role role = roleRepository.findByRoleCode(key)
                            .orElseThrow(() -> new IllegalArgumentException("Role no longer exists"));
                    upsertRole(source, schema, role.getId());
                } else if ("ROLE_PERMISSION".equals(type)) {
                    String[] parts = key.split(":");
                    reconcileRolePermissions(source, schema,
                            Long.valueOf(parts[0]), Long.valueOf(parts[1]));
                }
                completeSync(executionId, tenant, versionId, null);
            });
        } catch (Exception ex) {
            inNewTransaction(() -> failLatestSync(versionId, tenantId, ex));
        }
        return historyForTenant(tenantId).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Catalog retry did not create an execution"));
    }

    public List<CatalogSyncExecutionResponse> historyForTenant(Long tenantId) {
        return jdbcTemplate.query("""
                SELECT e.id, e.tenant_registry_id, c.version_number, e.status, e.attempt,
                       e.started_at, e.completed_at, e.error_message
                FROM catalog_sync_executions e
                JOIN catalog_versions c ON c.id=e.catalog_version_id
                WHERE e.tenant_registry_id=? ORDER BY e.created_on DESC
                """, (rs, rowNum) -> new CatalogSyncExecutionResponse(
                String.valueOf(rs.getLong("id")), rs.getLong("tenant_registry_id"),
                String.valueOf(rs.getLong("version_number")), currentCatalogVersion(),
                OperationStatus.valueOf(rs.getString("status")),
                timestamp(rs.getTimestamp("started_at")), timestamp(rs.getTimestamp("completed_at")),
                rs.getString("error_message"), Math.max(0, rs.getInt("attempt") - 1)), tenantId);
    }

    public CatalogSyncStatusResponse status() {
        List<TenantRegistry> tenants = completedTenants();
        String target = currentCatalogVersion();
        long synchronizedCount = tenants.stream()
                .filter(t -> target.equals(String.valueOf(t.getCatalogVersion()))).count();
        Long failures = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT DISTINCT ON (e.tenant_registry_id) e.tenant_registry_id, e.status
                    FROM catalog_sync_executions e
                    JOIN tenant_registry t ON t.id=e.tenant_registry_id
                    WHERE t.active=true AND t.provision_status='COMPLETED'
                    ORDER BY e.tenant_registry_id, e.created_on DESC
                ) latest WHERE status='FAILED'
                """, Long.class);
        long failed = failures == null ? 0 : failures;
        return new CatalogSyncStatusResponse(tenants.size(), synchronizedCount,
                Math.max(0, tenants.size() - synchronizedCount - failed), failed, target, LocalDateTime.now());
    }

    public void reconcileReleaseTarget(TenantRegistry tenant, String targetVersion) {
        long target = Long.parseLong(targetVersion);
        long current = Long.parseLong(currentCatalogVersion());
        if (target != current) {
            throw new IllegalArgumentException(
                    "Release catalog target must equal the current published catalog version");
        }
        Long catalogVersionId = jdbcTemplate.queryForObject("""
                SELECT id FROM catalog_versions WHERE version_number=?
                """, Long.class, target);
        Long executionId = inNewTransaction(() -> {
            Long id = startSync(catalogVersionId, tenant.getId());
            auditCatalog("CATALOG_RELEASE_SYNC_STARTED", tenant, id, catalogVersionId, "RUNNING", null);
            return id;
        });
        try {
            inNewTransaction(() -> {
                Organization organization = organizationRepository.findById(tenant.getOrganization().getId())
                        .orElseThrow(() -> new IllegalStateException("Organization not found"));
                reseedOrganizationNow(organization);
                completeSync(executionId, tenant, catalogVersionId, null);
                auditCatalog("CATALOG_RELEASE_SYNC_COMPLETED", tenant, executionId,
                        catalogVersionId, "SUCCESS", null);
            });
        } catch (Exception ex) {
            inNewTransaction(() -> {
                failLatestSync(catalogVersionId, tenant.getId(), ex);
                auditCatalog("CATALOG_RELEASE_SYNC_FAILED", tenant, executionId,
                        catalogVersionId, "FAILED", ex.getMessage());
            });
            throw ex;
        }
    }

    private String currentCatalogVersion() {
        Long version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version_number),0) FROM catalog_versions", Long.class);
        return String.valueOf(version == null ? 0 : version);
    }

    private LocalDateTime timestamp(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private Long recordCatalogChange(String changeType, String entityType, String entityKey) {
        return recordCatalogChange(changeType, entityType, entityKey, null);
    }

    private Long recordCatalogChange(String changeType, String entityType, String entityKey,
                                     Long targetTenantRegistryId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO catalog_versions
                    (change_type, entity_type, entity_key, target_tenant_registry_id)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """, Long.class, changeType, entityType, entityKey, targetTenantRegistryId);
    }

    private Long startSync(Long versionId, Long tenantId) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO catalog_sync_executions
                    (catalog_version_id, tenant_registry_id, status, attempt, started_at)
                SELECT ?, ?, 'RUNNING', COALESCE(MAX(attempt), 0) + 1, now()
                FROM catalog_sync_executions
                WHERE catalog_version_id=? AND tenant_registry_id=?
                RETURNING id
                """, Long.class, versionId, tenantId, versionId, tenantId);
    }

    private Long startSync(Long versionId, Long tenantId, int attempt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO catalog_sync_executions
                    (catalog_version_id, tenant_registry_id, status, attempt, started_at)
                VALUES (?, ?, 'RUNNING', ?, now()) RETURNING id
                """, Long.class, versionId, tenantId, attempt);
    }

    private void completeSync(Long executionId, TenantRegistry tenant, Long catalogVersion, String error) {
        jdbcTemplate.update("""
                UPDATE catalog_sync_executions SET status='SUCCESS', completed_at=now(), error_message=?
                WHERE id=?
                """, error, executionId);
        Long targetVersion = jdbcTemplate.queryForObject(
                "SELECT version_number FROM catalog_versions WHERE id=?", Long.class, catalogVersion);
        Boolean contiguous = jdbcTemplate.queryForObject("""
                SELECT NOT EXISTS (
                    SELECT 1
                    FROM catalog_versions c
                    WHERE c.version_number > COALESCE(
                            (SELECT catalog_version FROM tenant_registry WHERE id=?), 0)
                      AND c.version_number <= ?
                      AND (c.target_tenant_registry_id IS NULL
                           OR c.target_tenant_registry_id=?)
                      AND NOT EXISTS (
                          SELECT 1 FROM catalog_sync_executions e
                          WHERE e.catalog_version_id=c.id
                            AND e.tenant_registry_id=?
                            AND e.status='SUCCESS')
                )
                """, Boolean.class, tenant.getId(), targetVersion, tenant.getId(), tenant.getId());
        if (Boolean.TRUE.equals(contiguous)) {
            jdbcTemplate.update("""
                    UPDATE tenant_registry
                    SET catalog_version=?, version=version+1, updated_on=now()
                    WHERE id=?
                    """, targetVersion, tenant.getId());
            tenant.setCatalogVersion(targetVersion);
        }
    }

    private void failLatestSync(Long catalogVersion, Long tenantId, Exception ex) {
        jdbcTemplate.update("""
                UPDATE catalog_sync_executions SET status='FAILED', completed_at=now(), error_message=?
                WHERE catalog_version_id=? AND tenant_registry_id=? AND status='RUNNING'
                """, String.valueOf(ex.getMessage()), catalogVersion, tenantId);
    }

    private void auditCatalog(String eventType, TenantRegistry tenant, Long executionId,
                              Long catalogVersionId, String status, String error) {
        jdbcTemplate.update("""
                INSERT INTO operation_audit_events
                    (event_type, actor, tenant_identifier, entity_type, entity_id,
                     execution_id, status, target_version, error_message)
                VALUES (?, 'system', ?, 'CATALOG', ?, ?, ?, ?, ?)
                """, eventType, tenant.getTenantIdentifier(), String.valueOf(catalogVersionId),
                executionId, status, currentCatalogVersion(), sanitize(error));
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 2000 ? value.substring(0, 2000) : value;
    }

    private void afterCommitOrNow(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private <T> T inNewTransaction(Supplier<T> action) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> action.get());
    }

    private void inNewTransaction(Runnable action) {
        inNewTransaction(() -> {
            action.run();
            return null;
        });
    }

    private void reseedEntitlements(String schema, Organization organization) {
        Set<String> enabledFeatureCodes = entitledFeatureCodes(organization);
        String featureCodeList = enabledFeatureCodes.isEmpty()
                ? "''"
                : enabledFeatureCodes.stream().map(this::quoteLiteral).collect(Collectors.joining(","));
        String entitledMenusCte =
                "WITH RECURSIVE entitled AS ("
                        + "SELECT id, menu_type FROM " + schema + ".\"menus\" "
                        + "WHERE parent_menu_id IS NULL AND active = true "
                        + "AND (menu_scope = 'CORE' OR (menu_scope = 'SUBSCRIPTION' AND feature_id IN ("
                        + "SELECT id FROM " + schema + ".\"features\" WHERE feature_code IN (" + featureCodeList + ")))) "
                        + "UNION ALL "
                        + "SELECT m.id, m.menu_type FROM " + schema + ".\"menus\" m "
                        + "INNER JOIN entitled e ON m.parent_menu_id = e.id "
                        + "WHERE m.active = true"
                        + ") ";
        List<Long> entitledMenuIds = jdbcTemplate.query(entitledMenusCte + "SELECT id FROM entitled",
                (rs, rowNum) -> rs.getLong("id"));
        // Entitlement reconciliation never changes explicit permissions.
        if (entitledMenuIds.isEmpty()) {
                    jdbcTemplate.execute(
                        "UPDATE " + schema + ".\"organization_modules\" " +
                            "SET enabled = false " +
                            "WHERE organization_id = " + organization.getId());
                    return;
                }
                String menuIdList = entitledMenuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                jdbcTemplate.execute(
                    "UPDATE " + schema + ".\"organization_modules\" " +
                        "SET enabled = CASE WHEN menu_id IN (" + menuIdList + ") THEN true ELSE false END " +
                        "WHERE organization_id = " + organization.getId());
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"organization_modules\" "
                        + "(organization_id, menu_id, enabled, created_on, version) "
                        + "SELECT " + organization.getId() + ", id, true, now(), 0 "
                        + "FROM " + schema + ".\"menus\" WHERE id IN (" + menuIdList + ") "
                        + "ON CONFLICT (organization_id, menu_id) DO UPDATE SET enabled = true");
    }

    private void upsertMenuWithAncestors(String source, String schema, Menu menu) {
        if (menu.getParentMenu() != null) {
            upsertMenuWithAncestors(source, schema, menu.getParentMenu());
        }
        upsertMenu(source, schema, menu.getId());
    }

    private void upsertFeatureWithAncestors(String source, String schema, Feature feature) {
        if (feature.getParentFeature() != null) {
            upsertFeatureWithAncestors(source, schema, feature.getParentFeature());
        }
        upsertFeaturesByNaturalKey(source, schema, "s.id = " + feature.getId());
    }

    private void upsertFeaturesByNaturalKey(String source, String schema, String predicate) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"features\" "
                        + "(feature_code, feature_name, display_name, module, category, parent_feature_id, "
                        + "feature_key, description, icon, display_order, premium_feature, visible, "
                        + "default_enabled, active, remarks, created_by, created_on, updated_by, updated_on, version) "
                        + "SELECT s.feature_code, s.feature_name, s.display_name, s.module, s.category, NULL, "
                        + "s.feature_key, s.description, s.icon, s.display_order, s.premium_feature, s.visible, "
                        + "s.default_enabled, s.active, s.remarks, s.created_by, COALESCE(s.created_on, now()), "
                        + "s.updated_by, now(), 0 FROM " + source + ".\"features\" s "
                        + "WHERE " + predicate + " "
                        + "ON CONFLICT (feature_code) DO UPDATE SET "
                        + "feature_name=EXCLUDED.feature_name, display_name=EXCLUDED.display_name, "
                        + "module=EXCLUDED.module, category=EXCLUDED.category, feature_key=EXCLUDED.feature_key, "
                        + "description=EXCLUDED.description, icon=EXCLUDED.icon, "
                        + "display_order=EXCLUDED.display_order, premium_feature=EXCLUDED.premium_feature, "
                        + "visible=EXCLUDED.visible, default_enabled=EXCLUDED.default_enabled, "
                        + "active=EXCLUDED.active, remarks=EXCLUDED.remarks, updated_on=now()");
        jdbcTemplate.execute(
                "UPDATE " + schema + ".\"features\" t SET parent_feature_id=tp.id "
                        + "FROM " + source + ".\"features\" s "
                        + "LEFT JOIN " + source + ".\"features\" sp ON sp.id=s.parent_feature_id "
                        + "LEFT JOIN " + schema + ".\"features\" tp ON tp.feature_code=sp.feature_code "
                        + "WHERE t.feature_code=s.feature_code AND " + predicate);
    }

    private void copyOrgFacingMenus(String source, String schema) {
        upsertMenusByNaturalKey(source, schema, "s.menu_scope <> 'PLATFORM'");
    }

    private void upsertMenu(String source, String schema, Long menuId) {
        upsertMenusByNaturalKey(source, schema, "s.id = " + menuId);
    }

    private void upsertMenusByNaturalKey(String source, String schema, String predicate) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"menus\" "
                        + "(menu_code, menu_name, description, route, icon, menu_type, parent_menu_id, "
                        + "display_order, show_in_sidebar, active, default_page, menu_scope, feature_id, "
                        + "created_by, created_on, updated_by, updated_on, version) "
                        + "SELECT s.menu_code, s.menu_name, s.description, s.route, s.icon, s.menu_type, NULL, "
                        + "s.display_order, s.show_in_sidebar, s.active, s.default_page, s.menu_scope, tf.id, "
                        + "s.created_by, COALESCE(s.created_on, now()), s.updated_by, now(), 0 "
                        + "FROM " + source + ".\"menus\" s "
                        + "LEFT JOIN " + source + ".\"features\" sf ON sf.id=s.feature_id "
                        + "LEFT JOIN " + schema + ".\"features\" tf ON tf.feature_code=sf.feature_code "
                        + "WHERE " + predicate + " "
                        + "ON CONFLICT (menu_code) DO UPDATE SET "
                        + "menu_name = EXCLUDED.menu_name, "
                        + "description = EXCLUDED.description, "
                        + "route = EXCLUDED.route, "
                        + "icon = EXCLUDED.icon, "
                        + "menu_type = EXCLUDED.menu_type, "
                        + "display_order = EXCLUDED.display_order, "
                        + "show_in_sidebar = EXCLUDED.show_in_sidebar, "
                        + "active = EXCLUDED.active, "
                        + "default_page = EXCLUDED.default_page, "
                        + "menu_scope = EXCLUDED.menu_scope, "
                        + "feature_id = EXCLUDED.feature_id, "
                        + "updated_on = now()");
        jdbcTemplate.execute(
                "UPDATE " + schema + ".\"menus\" t SET parent_menu_id=tp.id "
                        + "FROM " + source + ".\"menus\" s "
                        + "LEFT JOIN " + source + ".\"menus\" sp ON sp.id=s.parent_menu_id "
                        + "LEFT JOIN " + schema + ".\"menus\" tp ON tp.menu_code=sp.menu_code "
                        + "WHERE t.menu_code=s.menu_code AND " + predicate);
    }

    private void upsertRole(String source, String schema, Long roleId) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"roles\" "
                        + "SELECT * FROM " + source + ".\"roles\" WHERE id = " + roleId + " "
                        + "ON CONFLICT (id) DO UPDATE SET "
                        + "role_code = EXCLUDED.role_code, "
                        + "role_name = EXCLUDED.role_name, "
                        + "description = EXCLUDED.description, "
                        + "role_type = EXCLUDED.role_type, "
                        + "dashboard_code = EXCLUDED.dashboard_code, "
                        + "system_role = EXCLUDED.system_role, "
                        + "active = EXCLUDED.active, "
                        + "display_order = EXCLUDED.display_order, "
                        + "updated_on = now()");
    }

    private List<TenantRegistry> completedTenants() {
        return tenantRegistryRepository.findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED)
                .stream()
                .filter(t -> StringUtils.hasText(t.getSchemaName()))
                .toList();
    }

    private String resolveSourceSchema() {
        try {
            String current = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
            if (StringUtils.hasText(current)) {
                return current;
            }
        } catch (Exception ignored) {
            // fall through to configured platform schema
        }
        return platformSchema;
    }

    private String quoteIdent(String ident) {
        if (!StringUtils.hasText(ident) || !ident.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid schema identifier");
        }
        return "\"" + ident + "\"";
    }

    private String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
