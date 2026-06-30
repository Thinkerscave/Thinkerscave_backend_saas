package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole_IdAndOrganization_Id(Long roleId, Long organizationId);

    Optional<RolePermission> findByRole_IdAndMenu_IdAndOrganization_Id(Long roleId, Long menuId, Long organizationId);

    boolean existsByRole_IdAndMenu_IdAndOrganization_Id(Long roleId, Long menuId, Long organizationId);

    @Query("SELECT rp FROM RolePermission rp JOIN FETCH rp.menu WHERE rp.role.id = :roleId AND rp.organization.id = :orgId ORDER BY rp.menu.displayOrder ASC")
    List<RolePermission> findByRoleWithMenu(@Param("roleId") Long roleId, @Param("orgId") Long orgId);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.organization.id = :orgId")
    void deleteAllByRoleAndOrganization(@Param("roleId") Long roleId, @Param("orgId") Long orgId);
}
