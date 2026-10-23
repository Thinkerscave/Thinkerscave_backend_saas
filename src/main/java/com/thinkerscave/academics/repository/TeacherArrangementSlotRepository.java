package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherArrangementSlot;
import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

@Repository
public interface TeacherArrangementSlotRepository extends JpaRepository<TeacherArrangementSlot, Long> {

    @Query("""
            SELECT DISTINCT slot.substituteTeacher.staffId
            FROM TeacherArrangementSlot slot
            WHERE slot.arrangement.absenceDate = :absenceDate
              AND slot.arrangement.status IN :statuses
              AND slot.timetableEntry.timetablePeriod.timetablePeriodId = :periodId
              AND (:excludeArrangementId IS NULL
                   OR slot.arrangement.teacherArrangementId <> :excludeArrangementId)
            """)
    Set<Long> findReservedSubstituteIds(
            @Param("absenceDate") LocalDate absenceDate,
            @Param("periodId") Long periodId,
            @Param("statuses") Collection<TeacherArrangementStatus> statuses,
            @Param("excludeArrangementId") Long excludeArrangementId);
}