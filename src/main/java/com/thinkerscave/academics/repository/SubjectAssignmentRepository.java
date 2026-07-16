package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssignmentRepository extends JpaRepository<SubjectAssignment, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndAcademicSection_SectionIdAndSubject_SubjectIdAndActiveTrue(
            Long yearId, Long classId, Long sectionId, Long subjectId);

    List<SubjectAssignment> findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderBySubject_SubjectNameAsc(
            Long yearId, Long classId);

    List<SubjectAssignment> findByTeacherIdAndAcademicYear_AcademicYearIdAndActiveTrue(
            Long teacherId, Long yearId);

    /** Used by the Staff dashboard's "My Classes Overview" widget — spans all academic years. */
    List<SubjectAssignment> findByTeacherIdAndActiveTrue(Long teacherId);

    @Query("SELECT SUM(sa.periodsPerWeek) FROM SubjectAssignment sa WHERE sa.teacherId = :teacherId AND sa.academicYear.academicYearId = :yearId AND sa.active = true")
    Integer sumPeriodsPerWeekByTeacher(@Param("teacherId") Long teacherId, @Param("yearId") Long yearId);
}
