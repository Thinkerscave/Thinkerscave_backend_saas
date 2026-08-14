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

    Optional<TeacherAllocation> findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
            Long sectionId, Long mappingId);

    @Query("""
            SELECT a FROM TeacherAllocation a
            JOIN FETCH a.section s
            JOIN FETCH s.academicClass c
            JOIN FETCH c.academicYear
            JOIN FETCH a.classSubjectMapping m
            JOIN FETCH m.subject
            WHERE a.teacherAllocationId = :id
            """)
    Optional<TeacherAllocation> findByIdWithDetails(@Param("id") Long id);

    List<TeacherAllocation> findBySection_SectionIdAndStatus(Long sectionId, TeacherAllocationStatus status);

    List<TeacherAllocation> findBySection_AcademicClass_AcademicYear_AcademicYearId(Long academicYearId);

    @Query("""
            SELECT a FROM TeacherAllocation a
            JOIN FETCH a.section s
            JOIN FETCH s.academicClass c
            JOIN FETCH a.classSubjectMapping m
            JOIN FETCH m.subject
            WHERE c.academicYear.academicYearId = :yearId
              AND a.active = true
            """)
    List<TeacherAllocation> findActiveWithDetailsByYear(@Param("yearId") Long yearId);

    List<TeacherAllocation> findByStatus(TeacherAllocationStatus status);

    List<TeacherAllocation> findByClassSubjectMapping_ClassSubjectMappingId(Long mappingId);

    long countByClassSubjectMapping_ClassSubjectMappingId(Long mappingId);

    long countByClassSubjectMapping_ClassSubjectMappingIdAndStatus(
            Long mappingId, TeacherAllocationStatus status);
}
