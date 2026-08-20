package com.thinkerscave.platform.service;

import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.MenuType;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.entity.SubscriptionFeatureOverride;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.repository.SubscriptionFeatureOverrideRepository;
import com.thinkerscave.platform.repository.SubscriptionPlanFeatureRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pushes platform catalog changes (menus / roles) into already-provisioned tenant
 * schemas and reseeds Owner/Admin entitlements when a feature is granted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantCatalogSyncService {

    private static final String ROLE_ORG_ADMIN_CODE = "ROLE_ADMIN";
    private static final String ROLE_ORG_OWNER_CODE = "ROLE_OWNER";

    private final JdbcTemplate jdbcTemplate;
    private final TenantRegistryRepository tenantRegistryRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanFeatureRepository planFeatureRepository;
    private final SubscriptionFeatureOverrideRepository overrideRepository;

    @Value("${app.tenancy.platform-schema:thinkerscave_dev}")
    private String platformSchema;

    @Transactional
    public void syncMenu(Menu menu) {
        if (menu == null || menu.getId() == null) {
            return;
        }
        if (menu.getMenuScope() == MenuScope.PLATFORM) {
            return;
        }
        String source = quoteIdent(resolveSourceSchema());
        for (TenantRegistry tenant : completedTenants()) {
            String schema = quoteIdent(tenant.getSchemaName());
            try {
                upsertMenuWithAncestors(source, schema, menu);
                applyMenuEntitlement(schema, tenant.getOrganization(), menu);
            } catch (Exception ex) {
                log.error("Failed to sync menu {} to tenant {}: {}",
                        menu.getMenuCode(), tenant.getTenantIdentifier(), ex.getMessage());
            }
        }
    }

    @Transactional
    public void syncRole(Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        String source = quoteIdent(resolveSourceSchema());
        for (TenantRegistry tenant : completedTenants()) {
            String schema = quoteIdent(tenant.getSchemaName());
            try {
                upsertRole(source, schema, role.getId());
            } catch (Exception ex) {
                log.error("Failed to sync role {} to tenant {}: {}",
                        role.getRoleCode(), tenant.getTenantIdentifier(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeMenu(Long menuId) {
        if (menuId == null) {
            return;
        }
        String sourceSchema = resolveSourceSchema();
        for (TenantRegistry tenant : completedTenants()) {
            if (tenant.getSchemaName() != null && tenant.getSchemaName().equalsIgnoreCase(sourceSchema)) {
                continue;
            }
            String schema = quoteIdent(tenant.getSchemaName());
            try {
                deleteMenuFromSchema(schema, menuId);
            } catch (Exception ex) {
                log.error("Failed to remove menu {} from tenant {}: {}",
                        menuId, tenant.getTenantIdentifier(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void reseedOrganization(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        tenantRegistryRepository.findByOrganization_Id(organization.getId()).ifPresent(tenant -> {
            if (!Boolean.TRUE.equals(tenant.getActive())
                    || tenant.getProvisionStatus() != ProvisionStatus.COMPLETED
                    || !StringUtils.hasText(tenant.getSchemaName())) {
                return;
            }
            try {
                String source = quoteIdent(resolveSourceSchema());
                String schema = quoteIdent(tenant.getSchemaName());
                copyOrgFacingMenus(source, schema);
                reseedEntitlements(schema, organization);
            } catch (Exception ex) {
                log.error("Failed to reseed entitlements for org {}: {}",
                        organization.getOrganizationCode(), ex.getMessage(), ex);
            }
        });
    }

    private void deleteMenuFromSchema(String schema, Long menuId) {
        jdbcTemplate.update("DELETE FROM " + schema + ".\"user_permissions\" WHERE menu_id = ?", menuId);
        jdbcTemplate.update("DELETE FROM " + schema + ".\"role_permissions\" WHERE menu_id = ?", menuId);
        jdbcTemplate.update("DELETE FROM " + schema + ".\"organization_modules\" WHERE menu_id = ?", menuId);
        jdbcTemplate.update("DELETE FROM " + schema + ".\"menus\" WHERE id = ?", menuId);
    }

    private void applyMenuEntitlement(String schema, Organization organization, Menu menu) {
        if (organization == null || !Boolean.TRUE.equals(menu.getActive())) {
            return;
        }
        if (!isEntitled(organization, menu)) {
            return;
        }
        grantMenu(schema, organization.getId(), menu);
    }

    private boolean isEntitled(Organization organization, Menu menu) {
        MenuScope scope = resolveTopLevelScope(menu);
        if (scope == MenuScope.CORE) {
            return true;
        }
        if (scope != MenuScope.SUBSCRIPTION) {
            return false;
        }
        Long featureId = resolveTopLevelFeatureId(menu);
        if (featureId == null) {
            return false;
        }
        return entitledFeatureIds(organization).contains(featureId);
    }

    private MenuScope resolveTopLevelScope(Menu menu) {
        Menu current = menu;
        while (current.getParentMenu() != null) {
            current = current.getParentMenu();
        }
        return current.getMenuScope() != null ? current.getMenuScope() : MenuScope.SUBSCRIPTION;
    }

    private Long resolveTopLevelFeatureId(Menu menu) {
        Menu current = menu;
        while (current.getParentMenu() != null) {
            current = current.getParentMenu();
        }
        return current.getFeature() != null ? current.getFeature().getId() : null;
    }

    private Set<Long> entitledFeatureIds(Organization organization) {
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganization_IdAndActiveTrue(organization.getId())
                .orElse(null);
        if (subscription == null || subscription.getSubscriptionPlan() == null) {
            return Set.of();
        }
        Set<Long> enabled = planFeatureRepository
                .findBySubscriptionPlan_IdAndEnabledTrueAndActiveTrue(subscription.getSubscriptionPlan().getId())
                .stream()
                .map(spf -> spf.getFeature().getId())
                .collect(Collectors.toCollection(HashSet::new));
        List<SubscriptionFeatureOverride> overrides = overrideRepository
                .findByOrganizationSubscription_IdAndActiveTrueOrderByCreatedOnDesc(subscription.getId());
        for (SubscriptionFeatureOverride override : overrides) {
            Long featureId = override.getFeature().getId();
            if (Boolean.TRUE.equals(override.getEnabled())) {
                enabled.add(featureId);
            } else {
                enabled.remove(featureId);
            }
        }
        return enabled;
    }

    private void grantMenu(String schema, Long organizationId, Menu menu) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"organization_modules\" "
                        + "(organization_id, menu_id, enabled, created_on, version) "
                        + "VALUES (" + organizationId + ", " + menu.getId() + ", true, now(), 0) "
                        + "ON CONFLICT (organization_id, menu_id) DO UPDATE SET enabled = true");
        if (menu.getMenuType() != MenuType.PAGE) {
            return;
        }
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"role_permissions\" "
                        + "(organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_on, version) "
                        + "SELECT " + organizationId + ", r.id, " + menu.getId() + ", true, true, true, now(), 0 "
                        + "FROM " + schema + ".\"roles\" r "
                        + "WHERE r.role_code IN ('" + ROLE_ORG_OWNER_CODE + "', '" + ROLE_ORG_ADMIN_CODE + "') "
                        + "ON CONFLICT (organization_id, role_id, menu_id) DO NOTHING");
    }

    private void reseedEntitlements(String schema, Organization organization) {
        Set<Long> enabledFeatureIds = entitledFeatureIds(organization);
        String featureIdList = enabledFeatureIds.isEmpty()
                ? "-1"
                : enabledFeatureIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String entitledMenusCte =
                "WITH RECURSIVE entitled AS ("
                        + "SELECT id, menu_type FROM " + schema + ".\"menus\" "
                        + "WHERE parent_menu_id IS NULL AND active = true "
                        + "AND (menu_scope = 'CORE' OR (menu_scope = 'SUBSCRIPTION' AND feature_id IN (" + featureIdList + "))) "
                        + "UNION ALL "
                        + "SELECT m.id, m.menu_type FROM " + schema + ".\"menus\" m "
                        + "INNER JOIN entitled e ON m.parent_menu_id = e.id "
                        + "WHERE m.active = true"
                        + ") ";
        List<Long> entitledMenuIds = jdbcTemplate.query(entitledMenusCte + "SELECT id FROM entitled",
                (rs, rowNum) -> rs.getLong("id"));
        if (entitledMenuIds.isEmpty()) {
            return;
        }
        String menuIdList = entitledMenuIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"organization_modules\" "
                        + "(organization_id, menu_id, enabled, created_on, version) "
                        + "SELECT " + organization.getId() + ", id, true, now(), 0 "
                        + "FROM " + schema + ".\"menus\" WHERE id IN (" + menuIdList + ") "
                        + "ON CONFLICT (organization_id, menu_id) DO UPDATE SET enabled = true");
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"role_permissions\" "
                        + "(organization_id, role_id, menu_id, can_view, can_manage, can_approve, created_on, version) "
                        + "SELECT " + organization.getId() + ", r.id, m.id, true, true, true, now(), 0 "
                        + "FROM " + schema + ".\"roles\" r "
                        + "CROSS JOIN " + schema + ".\"menus\" m "
                        + "WHERE r.role_code IN ('" + ROLE_ORG_OWNER_CODE + "', '" + ROLE_ORG_ADMIN_CODE + "') "
                        + "AND m.id IN (" + menuIdList + ") AND m.menu_type = 'PAGE' "
                        + "ON CONFLICT (organization_id, role_id, menu_id) DO NOTHING");
    }

    private void upsertMenuWithAncestors(String source, String schema, Menu menu) {
        if (menu.getParentMenu() != null) {
            upsertMenuWithAncestors(source, schema, menu.getParentMenu());
        }
        upsertMenu(source, schema, menu.getId());
    }

    private void copyOrgFacingMenus(String source, String schema) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"menus\" "
                        + "SELECT * FROM " + source + ".\"menus\" WHERE menu_scope <> 'PLATFORM' "
                        + "ON CONFLICT (id) DO UPDATE SET "
                        + "menu_code = EXCLUDED.menu_code, "
                        + "menu_name = EXCLUDED.menu_name, "
                        + "description = EXCLUDED.description, "
                        + "route = EXCLUDED.route, "
                        + "icon = EXCLUDED.icon, "
                        + "menu_type = EXCLUDED.menu_type, "
                        + "parent_menu_id = EXCLUDED.parent_menu_id, "
                        + "display_order = EXCLUDED.display_order, "
                        + "show_in_sidebar = EXCLUDED.show_in_sidebar, "
                        + "active = EXCLUDED.active, "
                        + "default_page = EXCLUDED.default_page, "
                        + "menu_scope = EXCLUDED.menu_scope, "
                        + "feature_id = EXCLUDED.feature_id, "
                        + "updated_on = now()");
    }

    private void upsertMenu(String source, String schema, Long menuId) {
        jdbcTemplate.execute(
                "INSERT INTO " + schema + ".\"menus\" "
                        + "SELECT * FROM " + source + ".\"menus\" WHERE id = " + menuId + " "
                        + "ON CONFLICT (id) DO UPDATE SET "
                        + "menu_code = EXCLUDED.menu_code, "
                        + "menu_name = EXCLUDED.menu_name, "
                        + "description = EXCLUDED.description, "
                        + "route = EXCLUDED.route, "
                        + "icon = EXCLUDED.icon, "
                        + "menu_type = EXCLUDED.menu_type, "
                        + "parent_menu_id = EXCLUDED.parent_menu_id, "
                        + "display_order = EXCLUDED.display_order, "
                        + "show_in_sidebar = EXCLUDED.show_in_sidebar, "
                        + "active = EXCLUDED.active, "
                        + "default_page = EXCLUDED.default_page, "
                        + "menu_scope = EXCLUDED.menu_scope, "
                        + "feature_id = EXCLUDED.feature_id, "
                        + "updated_on = now()");
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
}
