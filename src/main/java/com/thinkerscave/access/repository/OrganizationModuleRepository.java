package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.OrganizationModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationModuleRepository extends JpaRepository<OrganizationModule, Long> {

    List<OrganizationModule> findByOrganization_Id(Long organizationId);

    List<OrganizationModule> findByOrganization_IdAndEnabledTrue(Long organizationId);

    Optional<OrganizationModule> findByOrganization_IdAndMenu_Id(Long organizationId, Long menuId);

    boolean existsByOrganization_IdAndMenu_Id(Long organizationId, Long menuId);

    @Query("SELECT om.menu.id FROM OrganizationModule om WHERE om.organization.id = :orgId AND om.enabled = true")
    List<Long> findEnabledMenuIds(@Param("orgId") Long orgId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM OrganizationModule om WHERE om.menu.id = :menuId")
    void deleteByMenu_Id(@Param("menuId") Long menuId);
}
