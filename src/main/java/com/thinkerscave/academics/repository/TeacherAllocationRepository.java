package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAllocationRepository extends JpaRepository<TeacherAllocation, Long> {

    Optional<TeacherAllocation> findBySection_SectionIdAndAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(
            Long sectionId, Long mappingId);

    @Query("""
            SELECT a FROM TeacherAllocation a
            JOIN FETCH a.section s
            JOIN FETCH s.academicGroup A
            JOIN FETCH A.academicYear
            JOIN FETCH a.academicGroupSubjectMapping m
            JOIN FETCH m.subject
            WHERE a.teacherAllocationId = :id
            """)
    Optional<TeacherAllocation> findByIdWithDetails(@Param("id") Long id);

    List<TeacherAllocation> findBySection_SectionIdAndStatus(Long sectionId, TeacherAllocationStatus status);

    List<TeacherAllocation> findBySection_AcademicGroup_AcademicYear_AcademicYearId(Long academicYearId);

    @Query("""
            SELECT a FROM TeacherAllocation a
            JOIN FETCH a.section s
            JOIN FETCH s.academicGroup A
            JOIN FETCH a.academicGroupSubjectMapping m
            JOIN FETCH m.subject
            WHERE A.academicYear.academicYearId = :yearId
              AND a.active = true
            """)
    List<TeacherAllocation> findActiveWithDetailsByYear(@Param("yearId") Long yearId);

    List<TeacherAllocation> findByStatus(TeacherAllocationStatus status);

    List<TeacherAllocation> findByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(Long mappingId);

    long countByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(Long mappingId);

    long countByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingIdAndStatus(
            Long mappingId, TeacherAllocationStatus status);
}
