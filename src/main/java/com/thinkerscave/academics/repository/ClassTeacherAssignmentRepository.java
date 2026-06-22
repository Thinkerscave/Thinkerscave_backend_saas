package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTeacherAssignmentRepository extends JpaRepository<ClassTeacherAssignment, Long> {

    Optional<ClassTeacherAssignment> findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndAcademicSection_SectionIdAndActiveTrue(
            Long yearId, Long classId, Long sectionId);

    List<ClassTeacherAssignment> findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdOrderByCreatedOnDesc(
            Long yearId, Long classId);

    boolean existsByTeacherId(Long teacherId);
}
