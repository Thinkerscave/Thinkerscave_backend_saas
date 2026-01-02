package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Course;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Course} entities.
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Finds courses by organization.
     */
    List<Course> findByOrganization(Organisation organisation);

    /**
     * Finds a course by its unique code.
     */
    Optional<Course> findByCourseCode(String courseCode);

    /**
     * Finds active courses for an organization.
     */
    List<Course> findByOrganizationAndIsActiveTrue(Organisation organisation);
}
