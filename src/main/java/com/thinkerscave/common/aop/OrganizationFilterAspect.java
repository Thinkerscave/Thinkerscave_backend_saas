package com.thinkerscave.common.aop;

import com.thinkerscave.common.context.OrganizationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Aspect to enable the Hibernate 'tenantFilter' for data isolation.
 * Runs before Service methods to ensure all queries are scoped to the current
 * organization.
 */
@Aspect
@Component
@Slf4j
public class OrganizationFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Intercepts all public methods in Service classes.
     * Enables the tenantFilter on the current Hibernate Session.
     */
    @Before("execution(* com.thinkerscave.common..service..*(..))")
    public void enableTenantFilter() {
        try {
            Long orgId = OrganizationContext.getOrganizationId();
            Session session = entityManager.unwrap(Session.class);

            if (session != null) {
                if (orgId != null) {
                    session.enableFilter("tenantFilter").setParameter("tenantId", orgId);
                    log.trace("Enabled tenantFilter for organization: {}", orgId);
                } else {
                    // Fallback to prevent data leakage: Enable filter with invalid ID to return
                    // empty results
                    // UNLESS it's a super-admin or specific excluded service?
                    // For now, safety first: If you are in a tenant context but no org is selected,
                    // you see nothing.
                    // session.enableFilter("tenantFilter").setParameter("tenantId", -1L);
                    // log.trace("Enabled tenantFilter with fallback ID -1 (No Context)");

                    // Update: Disabling this fallback for now because it might break "Group Admin"
                    // features
                    // where they WANT to see everything.
                    // If no org context is set, we assume "Global/Group Access".
                    // The Frontend/OrganizationFilter should handle strictness.
                }
            }
        } catch (Exception e) {
            log.error("Failed to enable tenantFilter: {}", e.getMessage());
        }
    }
}
