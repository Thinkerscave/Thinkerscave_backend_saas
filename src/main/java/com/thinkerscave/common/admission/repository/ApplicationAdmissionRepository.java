package com.thinkerscave.common.admission.repository;

import com.thinkerscave.common.admission.domain.ApplicationAdmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing ApplicationAdmission entities in the database.
 * <p>
 * This interface provides CRUD operations and query methods for admission applications.
 *
 * @author Bibekananda Pradhan
 * @since 2025-08-05
 */
@Repository
public interface ApplicationAdmissionRepository extends JpaRepository<ApplicationAdmission, Long> {
}
