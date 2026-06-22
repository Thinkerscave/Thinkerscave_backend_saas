package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByChapter_ChapterIdAndActiveOrderByTopicNumberAsc(Long chapterId, Boolean active);

    List<Topic> findByChapter_Unit_Syllabus_SyllabusIdAndActiveTrue(Long syllabusId);
}
