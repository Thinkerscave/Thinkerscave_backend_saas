package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicGroupSubjectMappingRepository extends JpaRepository<AcademicGroupSubjectMapping, Long> {

    Optional<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(Long academicGroupId, Long subjectId);

    List<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);

    List<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupId(Long academicGroupId);

    List<AcademicGroupSubjectMapping> findBySubject_SubjectId(Long subjectId);

    List<AcademicGroupSubjectMapping> findBySubject_SubjectIdAndActiveTrue(Long subjectId);

    @Query("""
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.subject
            JOIN FETCH m.academicGroup
            WHERE m.academicGroup.academicGroupId = :academicGroupId AND m.active = true
            """)
    List<AcademicGroupSubjectMapping> findActiveWithSubjectByAcademicGroupId(@Param("academicGroupId") Long academicGroupId);

    long countBySubject_SubjectIdAndActiveTrue(Long subjectId);

    long countBySubject_AcademicYear_AcademicYearIdAndActiveTrue(Long yearId);

    @Query("""
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.subject sub
            JOIN FETCH m.academicGroup A
            WHERE A.academicYear.academicYearId = :yearId
              AND m.active = true
              AND A.active = true
              AND sub.active = true
            """)
    List<AcademicGroupSubjectMapping> findActiveForTimetableByYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT Count(DISTINCT s.subjectId) FROM Subject s
            WHERE s.academicYear.academicYearId = :yearId
              AND s.active = true
              AND NOT EXISTS (
                SELECT 1 FROM AcademicGroupSubjectMapping m
                WHERE m.subject = s AND m.active = true
              )
            """)
    long countUnmappedActiveSubjects(@Param("yearId") Long yearId);

    long countByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);
}
