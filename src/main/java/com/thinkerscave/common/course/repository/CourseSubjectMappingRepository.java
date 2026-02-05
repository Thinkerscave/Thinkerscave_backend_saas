package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Course;
import com.thinkerscave.common.course.domain.CourseSubjectMapping;
import com.thinkerscave.common.course.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSubjectMappingRepository extends JpaRepository<CourseSubjectMapping, Long> {

    List<CourseSubjectMapping> findByCourseAndIsActiveTrue(Course course);

    List<CourseSubjectMapping> findByCourseAndSemesterAndIsActiveTrue(Course course, Integer semester);

    Optional<CourseSubjectMapping> findByCourseAndSubjectAndIsActiveTrue(Course course, Subject subject);

    void deleteByCourseAndSubject(Course course, Subject subject);
}
