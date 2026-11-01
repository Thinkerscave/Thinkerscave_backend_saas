package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAllocationTeacherRepository extends JpaRepository<TeacherAllocationTeacher, Long> {

    List<TeacherAllocationTeacher> findByTeacherAllocation_TeacherAllocationIdOrderByEffectiveFromDesc(Long allocationId);

    Optional<TeacherAllocationTeacher> findByTeacherAllocation_TeacherAllocationIdAndRoleAndEffectiveToIsNull(
            Long allocationId, TeacherAllocationTeacherRole role);

    Optional<TeacherAllocationTeacher> findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
            Long allocationId, TeacherAllocationTeacherRole role);

    List<TeacherAllocationTeacher> findByStaff_StaffIdAndEffectiveToIsNull(Long staffId);

    @Query("""
            SELECT t FROM TeacherAllocationTeacher t
            JOIN FETCH t.teacherAllocation a
            JOIN FETCH a.academicGroupSubjectMapping m
            JOIN FETCH a.section s
            JOIN FETCH s.academicGroup A
            WHERE t.staff.staffId = :staffId
              AND t.active = true
              AND t.effectiveTo IS NULL
              AND a.active = true
              AND A.academicYear.academicYearId = :yearId
            """)
    List<TeacherAllocationTeacher> findActiveByStaffAndYear(
            @Param("staffId") Long staffId, @Param("yearId") Long yearId);

    @Query("""
            SELECT t FROM TeacherAllocationTeacher t
            JOIN FETCH t.staff
            JOIN FETCH t.teacherAllocation a
            JOIN FETCH a.academicGroupSubjectMapping m
            JOIN FETCH m.subject
            JOIN FETCH a.section s
            LEFT JOIN FETCH s.defaultResource
            JOIN FETCH s.academicGroup A
            WHERE A.academicYear.academicYearId = :yearId
              AND t.active = true
              AND t.effectiveTo IS NULL
              AND a.active = true
            """)
    List<TeacherAllocationTeacher> findActiveByYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT Count(DISTINCT t.staff.staffId) FROM TeacherAllocationTeacher t
            JOIN t.teacherAllocation a
            JOIN a.section s
            WHERE s.academicGroup.academicGroupId = :academicGroupId
              AND t.role = com.thinkerscave.academics.enums.TeacherAllocationTeacherRole.PRIMARY
              AND t.active = true
              AND t.effectiveTo IS NULL
              AND a.active = true
            """)
    long countDistinctPrimaryTeachersByAcademicGroup(@Param("academicGroupId") Long academicGroupId);

    @Query("""
            SELECT s.academicGroup.academicGroupId AS id, COUNT(DISTINCT t.staff.staffId) AS total
            FROM TeacherAllocationTeacher t
            JOIN t.teacherAllocation a
            JOIN a.section s
            JOIN s.academicGroup g
            WHERE g.academicYear.academicYearId = :yearId
              AND t.role = com.thinkerscave.academics.enums.TeacherAllocationTeacherRole.PRIMARY
              AND t.active = true
              AND t.effectiveTo IS NULL
              AND a.active = true
            GROUP BY s.academicGroup.academicGroupId
            """)
    List<IdCount> countDistinctPrimaryTeachersByAcademicYear(@Param("yearId") Long yearId);
}
