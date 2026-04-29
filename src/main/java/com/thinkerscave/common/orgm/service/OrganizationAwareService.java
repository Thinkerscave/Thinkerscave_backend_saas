package com.thinkerscave.common.orgm.service;

import com.thinkerscave.common.context.OrganizationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Base service providing organization-scoped data access utilities.
 * 
 * All services that need organization-level isolation should extend this class
 * or use its static methods to ensure queries are scoped to the current
 * organization.
 * 
 * @author System
 */
@Slf4j
@Service
public class OrganizationAwareService {

    /**
     * Get current organization ID from context.
     * 
     * @return Organization ID or null if not set
     */
    public static Long getCurrentOrganizationId() {
        return OrganizationContext.getOrganizationId();
    }

    /**
     * Get current organization ID, throwing exception if not set.
     * Use this when organization context is required for the operation.
     * 
     * @return Organization ID
     * @throws IllegalStateException if organization context is not set
     */
    public static Long requireOrganizationId() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException(
                    "Organization context is required but not set. " +
                            "Ensure X-Organization-ID header is provided.");
        }
        return orgId;
    }

    /**
     * Check if organization context is set.
     * 
     * @return true if organization ID is available
     */
    public static boolean hasOrganizationContext() {
        return OrganizationContext.isSet();
    }

    /**
     * Validate that an entity belongs to the current organization.
     * 
     * @param entityOrgId Organization ID from the entity
     * @param entityType  Type of entity (for error message)
     * @param entityId    ID of entity (for error message)
     * @throws SecurityException if entity doesn't belong to current organization
     */
    public static void validateOrganizationAccess(Long entityOrgId, String entityType, Object entityId) {
        Long currentOrgId = getCurrentOrganizationId();

        if (currentOrgId != null && !currentOrgId.equals(entityOrgId)) {
            log.warn("Access denied: Attempted to access {} ID {} belonging to org {} from org {}",
                    entityType, entityId, entityOrgId, currentOrgId);
            throw new SecurityException(
                    String.format("Access denied: %s does not belong to your organization", entityType));
        }
    }

    /**
     * Build WHERE clause for organization filtering.
     * Returns empty string if no organization context is set.
     * 
     * @param columnName Name of the organization_id column
     * @return SQL WHERE clause fragment or empty string
     */
    public static String buildOrgWhereClause(String columnName) {
        Long orgId = getCurrentOrganizationId();
        if (orgId == null) {
            return "";
        }
        return " AND " + columnName + " = " + orgId;
    }
}
