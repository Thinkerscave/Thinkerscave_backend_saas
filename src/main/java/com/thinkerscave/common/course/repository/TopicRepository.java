package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.Topic;
import com.thinkerscave.common.course.domain.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Topic} entities.
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * Finds topics by chapter, ordered by sequence.
     */
    List<Topic> findByChapterOrderByTopicNumberAsc(Chapter chapter);
}
