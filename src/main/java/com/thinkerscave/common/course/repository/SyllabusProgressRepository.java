package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.SyllabusProgress;
import com.thinkerscave.common.course.domain.Syllabus;
import com.thinkerscave.common.course.domain.Topic;
import com.thinkerscave.common.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link SyllabusProgress} entities.
 */
@Repository
public interface SyllabusProgressRepository extends JpaRepository<SyllabusProgress, Long> {

    /**
     * Finds progress for a student across all subjects.
     */
    List<SyllabusProgress> findByStudent(Student student);

    /**
     * Finds progress for a student in a specific syllabus.
     */
    List<SyllabusProgress> findByStudentAndSyllabus(Student student, Syllabus syllabus);

    /**
     * Finds specific topic progress for a student.
     */
    Optional<SyllabusProgress> findByStudentAndTopic(Student student, Topic topic);

    /**
     * Finds incomplete progress records for a student.
     */
    List<SyllabusProgress> findByStudentAndCompletedDateIsNull(Student student);
}
