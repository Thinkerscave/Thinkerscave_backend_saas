package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SyllabusCoverage;
import com.thinkerscave.academics.enums.CoverageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusCoverageRepository extends JpaRepository<SyllabusCoverage, Long> {

    Optional<SyllabusCoverage> findByTopic_TopicId(Long topicId);

    @Query("SELECT c FROM SyllabusCoverage c WHERE c.topic.chapter.unit.syllabus.syllabusId = :syllabusId")
    List<SyllabusCoverage> findBySyllabusId(@Param("syllabusId") Long syllabusId);

    @Query("SELECT COUNT(c) FROM SyllabusCoverage c WHERE c.topic.chapter.unit.syllabus.syllabusId = :syllabusId AND c.status = :status")
    long countByStatus(@Param("syllabusId") Long syllabusId, @Param("status") CoverageStatus status);
}
