package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAllocationRepository extends JpaRepository<TeacherAllocation, Long> {

    Optional<TeacherAllocation> findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
            Long sectionId, Long mappingId);

    List<TeacherAllocation> findBySection_SectionIdAndStatus(Long sectionId, TeacherAllocationStatus status);

    List<TeacherAllocation> findBySection_AcademicClass_AcademicYear_AcademicYearId(Long academicYearId);

    List<TeacherAllocation> findByStatus(TeacherAllocationStatus status);

    List<TeacherAllocation> findByClassSubjectMapping_ClassSubjectMappingId(Long mappingId);

    long countByClassSubjectMapping_ClassSubjectMappingId(Long mappingId);

    long countByClassSubjectMapping_ClassSubjectMappingIdAndStatus(
            Long mappingId, TeacherAllocationStatus status);
}
