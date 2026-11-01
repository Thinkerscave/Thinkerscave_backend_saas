package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherArrangement;
import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface TeacherArrangementRepository extends JpaRepository<TeacherArrangement, Long> {

    List<TeacherArrangement> findByAcademicYear_AcademicYearIdOrderByAbsenceDateDesc(Long academicYearId);

    List<TeacherArrangement> findByAcademicYear_AcademicYearIdAndAbsenceDateAndStatusOrderByTeacherArrangementIdAsc(
            Long academicYearId, LocalDate absenceDate, TeacherArrangementStatus status);

    boolean existsByAcademicYear_AcademicYearIdAndAbsentTeacher_StaffIdAndAbsenceDateAndStatusIn(
            Long academicYearId, Long absentTeacherId, LocalDate absenceDate,
            Collection<TeacherArrangementStatus> statuses);

    boolean existsByTimetableVersion_TimetableVersionId(Long versionId);
}