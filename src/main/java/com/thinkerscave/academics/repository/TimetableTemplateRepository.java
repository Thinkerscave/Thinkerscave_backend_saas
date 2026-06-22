package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableTemplateRepository extends JpaRepository<TimetableTemplate, Long> {

    List<TimetableTemplate> findByAcademicSchedule_ScheduleIdAndActiveOrderByTemplateNameAsc(Long scheduleId, Boolean active);

    List<TimetableTemplate> findByAcademicSchedule_ScheduleIdOrderByTemplateNameAsc(Long scheduleId);
}
