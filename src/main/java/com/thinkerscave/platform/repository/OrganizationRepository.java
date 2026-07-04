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
            WHERE o.active = true
            AND (:status IS NULL OR o.status = :status)
            AND (:institutionType IS NULL OR o.institutionType = :institutionType)
            AND (:customerId IS NULL OR o.customer.id = :customerId)
            AND (:search IS NULL OR LOWER(o.organizationName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(o.organizationCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(o.email) LIKE LOWER(CONCAT('%', :search, '%')))
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
}
