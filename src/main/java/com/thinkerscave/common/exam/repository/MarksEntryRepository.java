package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.MarksEntry;
import com.thinkerscave.common.exam.domain.MarksStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksEntryRepository extends JpaRepository<MarksEntry, Long>, JpaSpecificationExecutor<MarksEntry> {

    Optional<MarksEntry> findByExamIdAndSubjectIdAndStudentId(Long examId, Long subjectId, Long studentId);

    List<MarksEntry> findByExamIdAndSubjectId(Long examId, Long subjectId);

    List<MarksEntry> findByExamIdAndStudentId(Long examId, Long studentId);

    long countByExamIdAndStatus(Long examId, MarksStatus status);
}
