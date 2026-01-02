package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Subject} entities.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * Finds subjects by organization.
     */
    List<Subject> findByOrganization(Organisation organisation);

    /**
     * Finds a subject by its unique code.
     */
    Optional<Subject> findBySubjectCode(String subjectCode);

    /**
     * Finds active subjects for an organization.
     */
    List<Subject> findByOrganizationAndIsActiveTrue(Organisation organisation);
}
