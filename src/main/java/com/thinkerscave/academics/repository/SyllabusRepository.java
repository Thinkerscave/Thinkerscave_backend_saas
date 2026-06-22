package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Syllabus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    Optional<Syllabus> findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndSubject_SubjectIdAndActiveTrue(
            Long yearId, Long classId, Long subjectId);

    List<Syllabus> findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndActiveOrderByTitleAsc(
            Long yearId, Long classId, Boolean active);
}
