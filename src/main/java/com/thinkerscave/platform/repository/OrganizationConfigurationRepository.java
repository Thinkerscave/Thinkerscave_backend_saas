package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.OrganizationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationConfigurationRepository extends JpaRepository<OrganizationConfiguration, Long> {

    Optional<OrganizationConfiguration> findByOrganization_Id(Long organizationId);

    boolean existsByOrganization_Id(Long organizationId);
}
