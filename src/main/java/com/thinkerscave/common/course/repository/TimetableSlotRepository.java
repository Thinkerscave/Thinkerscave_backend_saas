package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.TimetableSlot;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    @EntityGraph(attributePaths = { "organization", "academicYear", "classEntity", "section", "subject", "teacher" })
    List<TimetableSlot> findByOrganizationAndAcademicYearAndIsActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(
            Organisation organization,
            AcademicYear academicYear);

    @EntityGraph(attributePaths = { "organization", "academicYear", "classEntity", "section", "subject", "teacher" })
    Optional<TimetableSlot> findBySlotIdAndOrganization(Long slotId, Organisation organization);

    @Query("""
            select count(slot) > 0
            from TimetableSlot slot
            where slot.organization = :organization
              and slot.academicYear = :academicYear
              and slot.classEntity = :classEntity
              and ((:section is null and slot.section is null) or slot.section = :section)
              and slot.dayOfWeek = :dayOfWeek
              and slot.periodNumber = :periodNumber
              and slot.isActive = true
              and (:excludeId is null or slot.slotId <> :excludeId)
            """)
    boolean existsClassSlotConflict(
            @Param("organization") Organisation organization,
            @Param("academicYear") AcademicYear academicYear,
            @Param("classEntity") ClassEntity classEntity,
            @Param("section") Section section,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("periodNumber") Integer periodNumber,
            @Param("excludeId") Long excludeId);

    @Query("""
            select count(slot) > 0
            from TimetableSlot slot
            where slot.organization = :organization
              and slot.academicYear = :academicYear
              and slot.teacher = :teacher
              and slot.dayOfWeek = :dayOfWeek
              and slot.periodNumber = :periodNumber
              and slot.isActive = true
              and (:excludeId is null or slot.slotId <> :excludeId)
            """)
    boolean existsTeacherSlotConflict(
            @Param("organization") Organisation organization,
            @Param("academicYear") AcademicYear academicYear,
            @Param("teacher") Staff teacher,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("periodNumber") Integer periodNumber,
            @Param("excludeId") Long excludeId);
}