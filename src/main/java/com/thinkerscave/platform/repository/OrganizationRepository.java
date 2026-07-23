package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByOrganizationCode(String organizationCode);

    Optional<Organization> findByOrganizationCode(String organizationCode);

    List<Organization> findByCustomer_IdAndActiveTrue(Long customerId);

    @Query("""
            SELECT o FROM Organization o
            JOIN FETCH o.tenantRegistry tr
            WHERE o.active = true
              AND (tr.active IS NULL OR tr.active = true)
              AND o.customer.ownerUserId = :ownerUserId
            """)
    List<Organization> findActiveByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Query("""
            SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END
            FROM Organization o
            JOIN o.tenantRegistry tr
            WHERE o.id = :organizationId
              AND o.active = true
              AND (tr.active IS NULL OR tr.active = true)
              AND o.customer.ownerUserId = :ownerUserId
            """)
    boolean existsActiveOwnedOrganization(
            @Param("ownerUserId") Long ownerUserId,
            @Param("organizationId") Long organizationId);

    @Query("""
            SELECT o FROM Organization o
            WHERE o.active = true
            AND (:status IS NULL OR o.status = :status)
            AND (:institutionType IS NULL OR o.institutionType = :institutionType)
            AND (:customerId IS NULL OR o.customer.id = :customerId)
            AND (:search IS NULL
                OR LOWER(o.organizationName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(o.organizationCode) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(o.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<Organization> searchOrganizations(
            @Param("status") OrganizationStatus status,
            @Param("institutionType") InstitutionType institutionType,
            @Param("customerId") Long customerId,
            @Param("search") String search,
            Pageable pageable);

    long countByActiveTrue();

    long countByStatus(OrganizationStatus status);

    long countByCustomer_IdAndActiveTrue(Long customerId);

    @Query("""
            SELECT DISTINCT o FROM Organization o
            LEFT JOIN FETCH o.tenantRegistry tr
            WHERE o.active = true
              AND o.status = com.thinkerscave.platform.enums.OrganizationStatus.ACTIVE
              AND tr IS NOT NULL
              AND (tr.active IS NULL OR tr.active = true)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(o.organizationName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(o.city, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(o.state, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(tr.tenantIdentifier, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    List<Organization> findPublicLoginOrganizations(@Param("search") String search);
}
