package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ProvisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRegistryRepository extends JpaRepository<TenantRegistry, Long> {

    Optional<TenantRegistry> findByTenantIdentifier(String tenantIdentifier);

    @Query("""
            SELECT t FROM TenantRegistry t
            WHERE t.active = true
            AND (
                LOWER(t.tenantIdentifier) = LOWER(:identifier)
                OR LOWER(REPLACE(t.tenantIdentifier, '-', '_')) = LOWER(:identifier)
                OR LOWER(REPLACE(t.tenantIdentifier, '_', '-')) = LOWER(:identifier)
            )
            """)
    Optional<TenantRegistry> findActiveByTenantIdentifierNormalized(@Param("identifier") String identifier);

    Optional<TenantRegistry> findBySchemaName(String schemaName);

    Optional<TenantRegistry> findByOrganization_Id(Long organizationId);

    boolean existsByTenantIdentifier(String tenantIdentifier);

    boolean existsBySchemaName(String schemaName);

    @Query("""
            SELECT t FROM TenantRegistry t
            WHERE t.active = true
            AND (:status IS NULL OR t.provisionStatus = :status)
            AND (:search IS NULL OR LOWER(t.tenantIdentifier) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(t.organization.organizationName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(t.schemaName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<TenantRegistry> searchTenants(
            @Param("status") ProvisionStatus status,
            @Param("search") String search,
            Pageable pageable);
}
