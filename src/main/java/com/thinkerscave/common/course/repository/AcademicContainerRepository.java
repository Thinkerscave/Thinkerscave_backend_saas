package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicContainer;
import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.enums.ContainerType;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link AcademicContainer} entities.
 */
@Repository
public interface AcademicContainerRepository extends JpaRepository<AcademicContainer, Long> {

    /**
     * Finds containers by organization and academic year.
     */
    List<AcademicContainer> findByOrganizationAndAcademicYear(Organisation organisation, AcademicYear academicYear);

    /**
     * Finds containers by type (e.g., CLASS, SECTION) for an organization.
     */
    List<AcademicContainer> findByOrganizationAndAcademicYearAndContainerType(Organisation organisation,
            AcademicYear academicYear, ContainerType containerType);

    /**
     * Finds child containers for a parent.
     */
    List<AcademicContainer> findByParentContainer(AcademicContainer parentContainer);

    /**
     * Finds a container by its unique code.
     */
    Optional<AcademicContainer> findByContainerCode(String containerCode);

    /**
     * Finds top-level containers (no parent) for an organization.
     */
    List<AcademicContainer> findByOrganizationAndAcademicYearAndParentContainerIsNull(Organisation organisation,
            AcademicYear academicYear);
}
