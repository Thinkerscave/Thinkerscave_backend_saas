package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.ClassSubjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectMappingRepository extends JpaRepository<ClassSubjectMapping, Long> {

    Optional<ClassSubjectMapping> findByAcademicClass_ClassIdAndSubject_SubjectId(Long classId, Long subjectId);

    List<ClassSubjectMapping> findByAcademicClass_ClassIdAndActiveTrue(Long classId);

    List<ClassSubjectMapping> findByAcademicClass_ClassId(Long classId);

    List<ClassSubjectMapping> findBySubject_SubjectId(Long subjectId);

    List<ClassSubjectMapping> findBySubject_SubjectIdAndActiveTrue(Long subjectId);

    @Query("""
            SELECT m FROM ClassSubjectMapping m
            JOIN FETCH m.subject
            JOIN FETCH m.academicClass
            WHERE m.academicClass.classId = :classId AND m.active = true
            """)
    List<ClassSubjectMapping> findActiveWithSubjectByClassId(@Param("classId") Long classId);

    long countBySubject_SubjectIdAndActiveTrue(Long subjectId);

    long countBySubject_AcademicYear_AcademicYearIdAndActiveTrue(Long yearId);

    @Query("""
            SELECT COUNT(DISTINCT s.subjectId) FROM Subject s
            WHERE s.academicYear.academicYearId = :yearId
              AND s.active = true
              AND NOT EXISTS (
                SELECT 1 FROM ClassSubjectMapping m
                WHERE m.subject = s AND m.active = true
              )
            """)
    long countUnmappedActiveSubjects(@Param("yearId") Long yearId);
}
