package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.SyllabusAccess;
import com.thinkerscave.common.course.domain.Syllabus;
import com.thinkerscave.common.usrm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link SyllabusAccess} entities.
 */
@Repository
public interface SyllabusAccessRepository extends JpaRepository<SyllabusAccess, Long> {

    /**
     * Finds access records for a user.
     */
    List<SyllabusAccess> findByUser(User user);

    /**
     * Finds access record for a user to a specific syllabus.
     */
    Optional<SyllabusAccess> findByUserAndSyllabus(User user, Syllabus syllabus);

    /**
     * Finds most accessed syllabus content.
     */
    List<SyllabusAccess> findTop10ByOrderByViewCountDesc();
}
