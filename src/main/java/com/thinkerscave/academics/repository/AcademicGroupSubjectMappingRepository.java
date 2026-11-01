package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicGroupSubjectMappingRepository extends JpaRepository<AcademicGroupSubjectMapping, Long> {

    Optional<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(Long academicGroupId, Long subjectId);

    List<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);

    List<AcademicGroupSubjectMapping> findByAcademicGroup_AcademicGroupId(Long academicGroupId);

    List<AcademicGroupSubjectMapping> findBySubject_SubjectIdAndActiveTrue(Long subjectId);

    @Query("""
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.subject
            JOIN FETCH m.academicGroup
            WHERE m.academicGroup.academicGroupId = :academicGroupId AND m.active = true
            """)
    List<AcademicGroupSubjectMapping> findActiveWithSubjectByAcademicGroupId(@Param("academicGroupId") Long academicGroupId);

    long countBySubject_SubjectIdAndActiveTrue(Long subjectId);

    @Query("""
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.academicGroup g
            JOIN FETCH g.academicYear y
            WHERE m.subject.subjectId = :subjectId AND m.active = true
            ORDER BY y.startDate DESC, g.displayOrder ASC
            """)
    List<AcademicGroupSubjectMapping> findActiveWithGroupAndYearBySubjectId(@Param("subjectId") Long subjectId);

    @Query("""
            SELECT Count(DISTINCT m.subject.subjectId) FROM AcademicGroupSubjectMapping m
            WHERE m.active = true AND m.academicGroup.academicYear.academicYearId = :yearId
            """)
    long countDistinctMappedSubjectsInYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT Count(DISTINCT m.subject.subjectId) FROM AcademicGroupSubjectMapping m
            WHERE m.active = true AND m.subject.active = true
              AND m.academicGroup.academicYear.academicYearId = :yearId
            """)
    long countDistinctActiveMappedSubjectsInYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT m.subject.subjectId AS id, COUNT(m) AS total FROM AcademicGroupSubjectMapping m
            WHERE m.active = true AND m.subject.subjectId IN :subjectIds
            GROUP BY m.subject.subjectId
            """)
    List<IdCount> countActiveBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);

    @Query("""
            SELECT a.academicGroupSubjectMapping.academicGroupSubjectMappingId AS id, COUNT(a) AS total
            FROM TeacherAllocation a
            WHERE a.active = true
              AND a.academicGroupSubjectMapping.academicGroupSubjectMappingId IN :mappingIds
            GROUP BY a.academicGroupSubjectMapping.academicGroupSubjectMappingId
            """)
    List<IdCount> countActiveAllocationsByMappingIds(@Param("mappingIds") Collection<Long> mappingIds);

    @Query("""
            SELECT e.teacherAllocation.academicGroupSubjectMapping.academicGroupSubjectMappingId AS id, COUNT(e) AS total
            FROM TimetableEntry e
            WHERE e.teacherAllocation.academicGroupSubjectMapping.academicGroupSubjectMappingId IN :mappingIds
            GROUP BY e.teacherAllocation.academicGroupSubjectMapping.academicGroupSubjectMappingId
            """)
    List<IdCount> countTimetableEntriesByMappingIds(@Param("mappingIds") Collection<Long> mappingIds);

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
            SELECT Count(s) FROM Subject s
            WHERE s.active = true
              AND NOT EXISTS (
                SELECT 1 FROM AcademicGroupSubjectMapping m
                WHERE m.subject = s AND m.active = true AND m.academicGroup.academicYear.academicYearId = :yearId
              )
            """)
    long countUnmappedActiveSubjects(@Param("yearId") Long yearId);


    @Query("""
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.academicGroup g
            JOIN FETCH m.subject s
            WHERE m.subject.subjectId IN :subjectIds AND m.active = true
            """)
    List<AcademicGroupSubjectMapping> findActiveWithGroupBySubjectIds(@Param("subjectIds") Collection<Long> subjectIds);

    @Query(value = """
            SELECT m FROM AcademicGroupSubjectMapping m
            JOIN FETCH m.academicGroup g
            JOIN FETCH g.academicYear y
            WHERE m.subject.subjectId = :subjectId AND m.active = true
              AND (LOWER(g.name) LIKE :pattern ESCAPE '!' OR LOWER(g.code) LIKE :pattern ESCAPE '!')
            ORDER BY y.startDate DESC, g.displayOrder ASC
            """,
            countQuery = """
            SELECT COUNT(m) FROM AcademicGroupSubjectMapping m
            JOIN m.academicGroup g
            WHERE m.subject.subjectId = :subjectId AND m.active = true
              AND (LOWER(g.name) LIKE :pattern ESCAPE '!' OR LOWER(g.code) LIKE :pattern ESCAPE '!')
            """)
    Page<AcademicGroupSubjectMapping> searchActiveBySubject(
            @Param("subjectId") Long subjectId, @Param("pattern") String pattern, Pageable pageable);

    long countByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);

    @Query("""
            SELECT m.academicGroup.academicGroupId AS id, COUNT(m) AS total
            FROM AcademicGroupSubjectMapping m
            WHERE m.active = true
              AND m.academicGroup.academicYear.academicYearId = :yearId
            GROUP BY m.academicGroup.academicGroupId
            """)
    List<IdCount> countActiveByAcademicGroupForYear(@Param("yearId") Long yearId);

}
