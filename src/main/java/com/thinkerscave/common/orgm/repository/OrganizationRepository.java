package com.thinkerscave.common.orgm.repository;

import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing and managing Organization entities.
 * Provides methods to fetch organization data by org code.
 *
 * @author Sandeep
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organisation, Long> {

    /** Finds an organization by its org code. */
    Optional<Organisation> findByOrgCode(String orgCode);


}