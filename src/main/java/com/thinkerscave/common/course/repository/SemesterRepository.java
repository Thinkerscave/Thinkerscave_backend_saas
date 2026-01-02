package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Semester;
import com.thinkerscave.common.course.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Semester} entities.
 */
@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    /**
     * Finds semesters by academic year.
     */
    List<Semester> findByAcademicYear(AcademicYear academicYear);

    /**
     * Finds active semesters for an academic year.
     */
    List<Semester> findByAcademicYearAndIsActiveTrue(AcademicYear academicYear);
}
