package com.thinkerscave.common.orgm.repository;

import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Finds all organizations that are marked as a 'group' and are currently active.
     * These are eligible to be parent organizations.
     * The method name is derived by Spring Data JPA to generate the query automatically.
     *
     * @return A list of parent-eligible Organisation entities.
     */
    List<Organisation> findByIsGroupTrueAndIsActiveTrue();


}