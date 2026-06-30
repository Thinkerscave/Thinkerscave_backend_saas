package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.StudentPeriodAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentPeriodAttendanceRepository extends JpaRepository<StudentPeriodAttendance, Long> {

    List<StudentPeriodAttendance> findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateAndPeriodIdOrderByRollNumber(
            Long orgId, Long classId, Long sectionId, LocalDate date, Long periodId);

    Optional<StudentPeriodAttendance> findByOrganizationIdAndStudentIdAndAttendanceDateAndPeriodId(
            Long orgId, Long studentId, LocalDate date, Long periodId);

    List<StudentPeriodAttendance> findByOrganizationIdAndStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAscPeriodNumberAsc(
            Long orgId, Long studentId, LocalDate from, LocalDate to);

    long countByOrganizationIdAndStudentIdAndAttendanceDateBetweenAndStatus(
            Long orgId, Long studentId, LocalDate from, LocalDate to, StudentAttendanceStatus status);

    @Query("""
            SELECT spa.periodId, spa.periodName, COUNT(spa) as total,
                   SUM(CASE WHEN spa.status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount
            FROM StudentPeriodAttendance spa
            WHERE spa.organizationId = :orgId
              AND spa.classId = :classId
              AND spa.sectionId = :sectionId
              AND spa.attendanceDate = :date
            GROUP BY spa.periodId, spa.periodName
            ORDER BY spa.periodId
            """)
    List<Object[]> getPeriodSummaryForClassDate(@Param("orgId") Long orgId,
            @Param("classId") Long classId, @Param("sectionId") Long sectionId,
            @Param("date") LocalDate date);

    @Modifying
    @Query("DELETE FROM StudentPeriodAttendance spa WHERE spa.organizationId = :orgId AND spa.classId = :classId AND spa.sectionId = :sectionId AND spa.attendanceDate = :date AND spa.periodId = :periodId")
    void deleteByClassSectionDatePeriod(@Param("orgId") Long orgId, @Param("classId") Long classId,
            @Param("sectionId") Long sectionId, @Param("date") LocalDate date, @Param("periodId") Long periodId);
}
