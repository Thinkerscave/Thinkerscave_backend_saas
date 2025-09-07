package com.thinkerscave.common.admission.repository;

import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing ApplicationAdmission entities in the database.
 * <p>
 * This interface provides CRUD operations and query methods for admission applications.
 *
 * @author Bibekananda Pradhan
 * @since 2025-08-05
 */
@Repository
public interface ApplicationAdmissionRepository extends JpaRepository<ApplicationAdmission, String> {
    Optional<ApplicationAdmission> findByApplicationId(String applicationId);

    /**
     * Finds all applications with the given application IDs.
     *
     * @param applicationIds List of application IDs to search for
     * @return List of found applications
     */
    List<ApplicationAdmission> findByApplicationIdIn(List<String> applicationIds);
}
