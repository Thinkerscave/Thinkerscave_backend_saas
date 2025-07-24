package com.thinkerscave.common.orgm.repository;


import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for OwnerDetails entity.
 * Provides methods to find owner info by organization or ownerCode.
 *
 * @author Sandeep
 */
@Repository
public interface OwnerDetailsRepository extends JpaRepository<OwnerDetails, Long> {

    /** Finds owner details by associated organization. */
    Optional<OwnerDetails> findByOrganization(Organisation organization);

    /** Finds owner details by owner code. */
    Optional<OwnerDetails> findByOwnerCode(String ownerCode);
}
