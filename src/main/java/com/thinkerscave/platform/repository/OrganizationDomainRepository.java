package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.OrganizationDomain;
import com.thinkerscave.platform.enums.DomainStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationDomainRepository extends JpaRepository<OrganizationDomain, Long> {

    Optional<OrganizationDomain> findByOrganization_Id(Long organizationId);

    Optional<OrganizationDomain> findBySubDomain(String subDomain);

    Optional<OrganizationDomain> findByCustomDomain(String customDomain);

    boolean existsBySubDomain(String subDomain);

    boolean existsByCustomDomain(String customDomain);

    List<OrganizationDomain> findByStatusAndActiveTrue(DomainStatus status);
}
