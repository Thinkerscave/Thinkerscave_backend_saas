package com.thinkerscave.attendance.repository;

import com.thinkerscave.attendance.entity.StudentAttendance;
import com.thinkerscave.attendance.enums.StudentAttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long>,
        JpaSpecificationExecutor<StudentAttendance> {

    // ─── Fetch for marking ────────────────────────────────────────────────

    List<StudentAttendance> findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateOrderByRollNumber(
            Long orgId, Long classId, Long sectionId, LocalDate date);

    List<StudentAttendance> findByOrganizationIdAndClassIdAndAttendanceDateOrderByRollNumber(
            Long orgId, Long classId, LocalDate date);

    Optional<StudentAttendance> findByOrganizationIdAndStudentIdAndAttendanceDate(
            Long orgId, Long studentId, LocalDate date);

    // ─── Student history ──────────────────────────────────────────────────

    Page<StudentAttendance> findByOrganizationIdAndStudentIdOrderByAttendanceDateDesc(
            Long orgId, Long studentId, Pageable pageable);

    List<StudentAttendance> findByOrganizationIdAndStudentIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long orgId, Long studentId, LocalDate from, LocalDate to);

    // ─── Summary queries ──────────────────────────────────────────────────

    long countByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateAndStatus(
            Long orgId, Long classId, Long sectionId, LocalDate date, StudentAttendanceStatus status);

    long countByOrganizationIdAndClassIdAndSectionIdAndAttendanceDate(
            Long orgId, Long classId, Long sectionId, LocalDate date);

    @Query("""
            SELECT sa.status, COUNT(sa)
            FROM StudentAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.studentId = :studentId
              AND sa.attendanceDate BETWEEN :from AND :to
            GROUP BY sa.status
            """)
    List<Object[]> countByStatusForStudent(@Param("orgId") Long orgId,
            @Param("studentId") Long studentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT sa.attendanceDate, COUNT(DISTINCT sa.studentId) as totalStudents,
                   SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount
            FROM StudentAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.attendanceDate BETWEEN :from AND :to
            GROUP BY sa.attendanceDate
            ORDER BY sa.attendanceDate
            """)
    List<Object[]> getDailyAttendanceSummary(@Param("orgId") Long orgId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            SELECT sa.classId, sa.className, sa.sectionId, sa.sectionName,
                   COUNT(sa) as total,
                   SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount
            FROM StudentAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.attendanceDate = :date
            GROUP BY sa.classId, sa.className, sa.sectionId, sa.sectionName
            """)
    List<Object[]> getClassWiseSummaryForDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    // ─── Dashboard ────────────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(DISTINCT ac.classId)
            FROM AcademicClass ac
            WHERE NOT EXISTS (
                SELECT 1 FROM StudentAttendance sa2
                WHERE sa2.organizationId = :orgId
                  AND sa2.classId = ac.classId
                  AND sa2.attendanceDate = :date
            ) AND ac.active = true
            """)
    long countClassesWithPendingAttendance(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    // ─── Copy from previous day ───────────────────────────────────────────

    List<StudentAttendance> findByOrganizationIdAndClassIdAndSectionIdAndAttendanceDateBetweenOrderByRollNumber(
            Long orgId, Long classId, Long sectionId, LocalDate from, LocalDate to);

    // ─── Defaulter list ───────────────────────────────────────────────────

    @Query("""
            SELECT sa.studentId, sa.studentName, sa.className, sa.sectionName,
                   COUNT(sa) as totalDays,
                   SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) as presentDays
            FROM StudentAttendance sa
            WHERE sa.organizationId = :orgId
              AND sa.attendanceDate BETWEEN :from AND :to
            GROUP BY sa.studentId, sa.studentName, sa.className, sa.sectionName
            HAVING (CAST(SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) AS double) / COUNT(sa)) * 100 < :threshold
            ORDER BY (CAST(SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) AS double) / COUNT(sa)) ASC
            """)
    List<Object[]> getDefaulterList(@Param("orgId") Long orgId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("threshold") double threshold);

    @Modifying
    @Query("DELETE FROM StudentAttendance sa WHERE sa.organizationId = :orgId AND sa.classId = :classId AND sa.sectionId = :sectionId AND sa.attendanceDate = :date")
    void deleteByClassSectionDate(@Param("orgId") Long orgId, @Param("classId") Long classId,
            @Param("sectionId") Long sectionId, @Param("date") LocalDate date);

    // ─── Dashboard org-wide counts ────────────────────────────────────────

    long countByOrganizationIdAndAttendanceDateAndStatus(Long orgId, LocalDate date, StudentAttendanceStatus status);
}
