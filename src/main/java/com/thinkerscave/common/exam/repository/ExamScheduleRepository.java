package com.thinkerscave.common.exam.repository;

import com.thinkerscave.common.exam.domain.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {

    List<ExamSchedule> findByExamIdOrderByExamDateAscStartTimeAsc(Long examId);

    List<ExamSchedule> findByExamDateBetween(LocalDate from, LocalDate to);
}
