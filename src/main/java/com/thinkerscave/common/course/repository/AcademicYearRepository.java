package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link AcademicYear} entities.
 */
@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    List<AcademicYear> findByOrganization(Organisation organisation);

    /**
     * Finds the current academic year for an organization.
     */
    Optional<AcademicYear> findByOrganizationAndIsCurrentTrue(Organisation organisation);

    /**
     * Finds an academic year by its code for an organization.
     */
    Optional<AcademicYear> findByOrganizationAndYearCode(Organisation organisation, String yearCode);
}
