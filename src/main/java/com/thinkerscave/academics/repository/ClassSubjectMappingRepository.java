package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.ClassSubjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectMappingRepository extends JpaRepository<ClassSubjectMapping, Long> {

    Optional<ClassSubjectMapping> findByAcademicClass_ClassIdAndSubject_SubjectId(Long classId, Long subjectId);

    List<ClassSubjectMapping> findByAcademicClass_ClassIdAndActiveTrue(Long classId);

    List<ClassSubjectMapping> findByAcademicClass_ClassId(Long classId);

    List<ClassSubjectMapping> findBySubject_SubjectId(Long subjectId);
}
