package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Syllabus;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.enums.SyllabusStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link Syllabus} entities.
 */
@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    /**
     * Finds syllabus by subject.
     */
    List<Syllabus> findBySubject(Subject subject);

    /**
     * Finds syllabus by its unique code.
     */
    Optional<Syllabus> findBySyllabusCode(String syllabusCode);

    /**
     * Finds the published syllabus for a subject.
     */
    Optional<Syllabus> findBySubjectAndStatus(Subject subject, SyllabusStatus status);

    /**
     * Finds all versions of a syllabus for a subject.
     */
    List<Syllabus> findBySubjectOrderByVersionDesc(Subject subject);
}
