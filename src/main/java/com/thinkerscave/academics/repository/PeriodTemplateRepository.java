package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.PeriodTemplate;
import com.thinkerscave.academics.enums.PeriodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface PeriodTemplateRepository extends JpaRepository<PeriodTemplate, Long> {

    List<PeriodTemplate> findByTimetableTemplate_TemplateIdAndActiveOrderByPeriodNumberAsc(Long templateId, Boolean active);

    List<PeriodTemplate> findByTimetableTemplate_TemplateIdOrderByPeriodNumberAsc(Long templateId);

    @Query("SELECT COUNT(p) FROM PeriodTemplate p WHERE p.timetableTemplate.templateId = :templateId " +
           "AND p.active = true AND p.periodType = :type " +
           "AND ((p.startTime < :endTime AND p.endTime > :startTime))")
    long countOverlapping(@Param("templateId") Long templateId,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("type") PeriodType type);
}
