package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser_IdAndActiveTrue(Long userId);

    Optional<UserRole> findByUser_IdAndRole_IdAndActiveTrue(Long userId, Long roleId);

    boolean existsByUser_IdAndRole_IdAndActiveTrue(Long userId, Long roleId);

    Optional<UserRole> findByUser_IdAndPrimaryRoleTrue(Long userId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId AND ur.active = true")
    List<UserRole> findActiveRolesWithDetails(@Param("userId") Long userId);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.id = :roleId AND ur.active = true")
    long countActiveUsersByRole(@Param("roleId") Long roleId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.user WHERE ur.role.id = :roleId AND ur.active = true")
    List<UserRole> findActiveAssignmentsByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT COUNT(ur) FROM UserRole ur JOIN ur.role r WHERE r.roleType = com.thinkerscave.access.enums.RoleType.ORGANIZATION_ADMIN AND ur.user.organizationId = :organizationId AND ur.active = true")
    long countActiveAdminsByOrganization(@Param("organizationId") Long organizationId);

    @Modifying
    @Query("UPDATE UserRole ur SET ur.active = false WHERE ur.user.id = :userId")
    void deactivateAllForUser(@Param("userId") Long userId);
}
