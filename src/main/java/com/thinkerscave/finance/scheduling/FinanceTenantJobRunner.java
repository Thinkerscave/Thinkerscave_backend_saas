package com.thinkerscave.finance.scheduling;

import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Runs a unit of work once per completed tenant schema.
 * {@link TenantContext} must be set before the {@code REQUIRES_NEW} transaction starts,
 * because Hibernate resolves the tenant schema when the session opens and cannot
 * switch it mid-transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceTenantJobRunner {

    private final TenantRegistryRepository tenantRegistryRepository;
    private final ObjectProvider<FinanceTenantJobRunner> selfProvider;

    public void forEachTenant(String jobName, Consumer<TenantRegistry> work) {
        List<TenantRegistry> tenants = tenantRegistryRepository
                .findByActiveTrueAndProvisionStatus(ProvisionStatus.COMPLETED)
                .stream()
                .filter(t -> StringUtils.hasText(t.getSchemaName()))
                .toList();
        log.info("{}: iterating {} tenant schema(s)", jobName, tenants.size());
        FinanceTenantJobRunner self = selfProvider.getObject();
        for (TenantRegistry tenant : tenants) {
            Long organizationId = null;
            try {
                if (tenant.getOrganization() != null) {
                    organizationId = tenant.getOrganization().getId();
                }
            } catch (Exception ex) {
                log.debug("{}: could not resolve organization for {}: {}",
                        jobName, tenant.getTenantIdentifier(), ex.getMessage());
            }
            try {
                self.runInTenant(tenant.getSchemaName(), organizationId, () -> work.accept(tenant));
            } catch (Exception ex) {
                log.warn("{} failed for tenant {} ({}): {}",
                        jobName,
                        tenant.getTenantIdentifier(),
                        tenant.getSchemaName(),
                        ex.getMessage());
            }
        }
    }

    public void runInTenant(String schemaName, Long organizationId, Runnable work) {
        String previousTenant = TenantContext.getTenant();
        Long previousOrg = OrganizationContext.getOrganizationId();
        try {
            TenantContext.setTenant(schemaName);
            if (organizationId != null) {
                OrganizationContext.setOrganizationId(organizationId);
            } else {
                OrganizationContext.clear();
            }
            selfProvider.getObject().runInNewTransaction(work);
        } finally {
            if (previousTenant != null) {
                TenantContext.setTenant(previousTenant);
            } else {
                TenantContext.clear();
            }
            if (previousOrg != null) {
                OrganizationContext.setOrganizationId(previousOrg);
            } else {
                OrganizationContext.clear();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runInNewTransaction(Runnable work) {
        work.run();
    }
}
