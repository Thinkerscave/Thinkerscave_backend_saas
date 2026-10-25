package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicGroupSubjectMappingSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface AcademicGroupSubjectMappingSectionRepository
        extends JpaRepository<AcademicGroupSubjectMappingSection, Long> {

    List<AcademicGroupSubjectMappingSection> findByMapping_AcademicGroupSubjectMappingId(Long mappingId);

    @Query("""
            SELECT ms FROM AcademicGroupSubjectMappingSection ms
            JOIN FETCH ms.section s
            WHERE ms.mapping.academicGroupSubjectMappingId IN :mappingIds
            """)
    List<AcademicGroupSubjectMappingSection> findWithSectionByMappingIds(@Param("mappingIds") Collection<Long> mappingIds);

    @Query("""
            SELECT ms.mapping.academicGroupSubjectMappingId AS id, COUNT(ms) AS total
            FROM AcademicGroupSubjectMappingSection ms
            WHERE ms.mapping.academicGroupSubjectMappingId IN :mappingIds
            GROUP BY ms.mapping.academicGroupSubjectMappingId
            """)
    List<IdCount> countByMappingIds(@Param("mappingIds") Collection<Long> mappingIds);

    void deleteByMapping_AcademicGroupSubjectMappingId(Long mappingId);
}
