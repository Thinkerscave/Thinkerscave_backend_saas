package com.thinkerscave.common.orgm.repository;

import com.thinkerscave.common.orgm.domain.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OrganizationUser entity.
 * Queries run in the CURRENT TENANT SCHEMA (set by TenantFilter).
 * 
 * @author System
 */
@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, Long> {

    /**
     * Find all organizations a user belongs to.
     * 
     * @param userId User ID
     * @return List of organization memberships
     */
    List<OrganizationUser> findByUserId(Long userId);

    /**
     * Find all active organizations a user belongs to.
     * 
     * @param userId User ID
     * @return List of active organization memberships
     */
    List<OrganizationUser> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * Find all users in an organization.
     * 
     * @param organizationId Organization ID
     * @return List of users in this organization
     */
    List<OrganizationUser> findByOrganizationId(Long organizationId);

    /**
     * Find all active users in an organization.
     * 
     * @param organizationId Organization ID
     * @return List of active users in this organization
     */
    List<OrganizationUser> findByOrganizationIdAndIsActive(Long organizationId, Boolean isActive);

    /**
     * Find specific user-organization membership.
     * 
     * @param organizationId Organization ID
     * @param userId         User ID
     * @return Optional membership record
     */
    Optional<OrganizationUser> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    /**
     * Check if user belongs to organization (active membership).
     * 
     * @param organizationId Organization ID
     * @param userId         User ID
     * @return true if user has active membership
     */
    @Query("SELECT CASE WHEN COUNT(ou) > 0 THEN true ELSE false END " +
            "FROM OrganizationUser ou " +
            "WHERE ou.organizationId = :orgId AND ou.userId = :userId AND ou.isActive = true")
    boolean userBelongsToOrganization(@Param("orgId") Long organizationId,
            @Param("userId") Long userId);

    /**
     * Get user's role in specific organization.
     * 
     * @param organizationId Organization ID
     * @param userId         User ID
     * @return Role name or null if not found
     */
    @Query("SELECT ou.roleName FROM OrganizationUser ou " +
            "WHERE ou.organizationId = :orgId AND ou.userId = :userId AND ou.isActive = true")
    Optional<String> getUserRoleInOrganization(@Param("orgId") Long organizationId,
            @Param("userId") Long userId);

    /**
     * Count active users in an organization.
     * 
     * @param organizationId Organization ID
     * @return Number of active users
     */
    @Query("SELECT COUNT(ou) FROM OrganizationUser ou " +
            "WHERE ou.organizationId = :orgId AND ou.isActive = true")
    Long countActiveUsersInOrganization(@Param("orgId") Long organizationId);
}
