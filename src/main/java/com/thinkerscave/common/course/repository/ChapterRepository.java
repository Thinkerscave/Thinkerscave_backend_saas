package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Chapter;
import com.thinkerscave.common.course.domain.Syllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Chapter} entities.
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * Finds chapters by syllabus, ordered by sequence.
     */
    List<Chapter> findBySyllabusOrderByChapterNumberAsc(Syllabus syllabus);

    /**
     * Finds a branch by its name for a syllabus (if unique names are used).
     */
    List<Chapter> findBySyllabusAndChapterName(Syllabus syllabus, String chapterName);
}
