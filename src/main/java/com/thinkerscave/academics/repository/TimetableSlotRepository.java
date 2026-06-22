package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableSlot;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    List<TimetableSlot> findByAcademicClass_ClassIdAndAcademicSection_SectionIdAndActiveOrderByDayOfWeekAscPeriodTemplate_PeriodNumberAsc(
            Long classId, Long sectionId, Boolean active);

    List<TimetableSlot> findByAcademicClass_ClassIdAndAcademicSection_SectionIdOrderByDayOfWeekAscPeriodTemplate_PeriodNumberAsc(
            Long classId, Long sectionId);

    @Query("SELECT COUNT(ts) FROM TimetableSlot ts WHERE ts.subjectAssignment.teacherId = :teacherId " +
           "AND ts.dayOfWeek = :day AND ts.periodTemplate.periodTemplateId = :periodId AND ts.active = true " +
           "AND ts.academicYear.academicYearId = :yearId")
    long countTeacherConflict(@Param("teacherId") Long teacherId,
                              @Param("day") DayOfWeek day,
                              @Param("periodId") Long periodId,
                              @Param("yearId") Long yearId);
}
