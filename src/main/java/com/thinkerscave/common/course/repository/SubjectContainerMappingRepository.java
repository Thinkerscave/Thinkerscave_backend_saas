package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.SubjectContainerMapping;
import com.thinkerscave.common.course.domain.AcademicContainer;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link SubjectContainerMapping} entities.
 */
@Repository
public interface SubjectContainerMappingRepository extends JpaRepository<SubjectContainerMapping, Long> {

    /**
     * Finds subjects mapped to a specific container.
     */
    List<SubjectContainerMapping> findByContainer(AcademicContainer container);

    /**
     * Finds containers where a specific subject is taught.
     */
    List<SubjectContainerMapping> findBySubject(Subject subject);

    /**
     * Finds mappings for a container in a specific academic year.
     */
    List<SubjectContainerMapping> findByContainerAndAcademicYear(AcademicContainer container,
            AcademicYear academicYear);
}
