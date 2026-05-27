package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSubjectRepository extends JpaRepository<ExamSubject, Long> {
    List<ExamSubject> findByExamId(Long examId);
    void deleteByExamId(Long examId);
}
