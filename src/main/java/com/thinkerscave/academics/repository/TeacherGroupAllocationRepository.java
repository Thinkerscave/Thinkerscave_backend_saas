package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherGroupAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherGroupAllocationRepository extends JpaRepository<TeacherGroupAllocation, Long> {

    Optional<TeacherGroupAllocation> findByMapping_AcademicGroupSubjectMappingId(Long mappingId);

    @Query("""
            SELECT t FROM TeacherGroupAllocation t
            JOIN FETCH t.staff
            JOIN t.mapping m
            JOIN m.academicGroup g
            WHERE g.academicYear.academicYearId = :yearId AND t.active = true AND m.active = true
            """)
    List<TeacherGroupAllocation> findActiveByYear(@Param("yearId") Long yearId);

    @Query("""
            SELECT t FROM TeacherGroupAllocation t
            JOIN FETCH t.staff
            WHERE t.mapping.academicGroupSubjectMappingId IN :mappingIds AND t.active = true
            """)
    List<TeacherGroupAllocation> findActiveByMappingIds(@Param("mappingIds") Collection<Long> mappingIds);
}
