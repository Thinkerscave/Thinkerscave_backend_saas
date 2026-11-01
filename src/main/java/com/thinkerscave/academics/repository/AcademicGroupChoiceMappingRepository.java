package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicGroupChoiceMappingRepository extends JpaRepository<AcademicGroupChoiceMapping, Long> {

    Optional<AcademicGroupChoiceMapping> findByAcademicGroup_AcademicGroupIdAndChoiceGroup_SubjectChoiceGroupId(
            Long academicGroupId, Long choiceGroupId);

    List<AcademicGroupChoiceMapping> findByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);

    List<AcademicGroupChoiceMapping> findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByChoiceGroup_NameAsc(
            Long academicGroupId);

    List<AcademicGroupChoiceMapping> findByAcademicGroup_AcademicGroupId(Long academicGroupId);

    List<AcademicGroupChoiceMapping> findByChoiceGroup_SubjectChoiceGroupIdAndActiveTrue(Long choiceGroupId);

    long countByChoiceGroup_SubjectChoiceGroupIdAndActiveTrue(Long choiceGroupId);

    @Query("""
            SELECT m FROM AcademicGroupChoiceMapping m
            JOIN FETCH m.choiceGroup g
            JOIN FETCH m.academicGroup a
            WHERE m.academicGroup.academicGroupId = :academicGroupId AND m.active = true
            ORDER BY LOWER(g.name), g.subjectChoiceGroupId
            """)
    List<AcademicGroupChoiceMapping> findActiveDetailedByAcademicGroupId(
            @Param("academicGroupId") Long academicGroupId);

    @Query("""
            SELECT COUNT(m) FROM AcademicGroupChoiceMapping m
            WHERE m.choiceGroup.subjectChoiceGroupId = :choiceGroupId AND m.active = true
            """)
    long countActiveMappings(@Param("choiceGroupId") Long choiceGroupId);

    @Query("""
            SELECT m FROM AcademicGroupChoiceMapping m
            JOIN FETCH m.choiceGroup
            JOIN FETCH m.academicGroup
            WHERE m.academicGroup.academicYear.academicYearId = :yearId AND m.active = true
            """)
    List<AcademicGroupChoiceMapping> findActiveDetailedByYear(@Param("yearId") Long yearId);

    default List<AcademicGroupChoiceMapping> findActiveDetailedByYearAndGroupIds(
            Long yearId, Collection<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return List.of();
        return findActiveDetailedByYear(yearId).stream()
                .filter(mapping -> groupIds.contains(mapping.getAcademicGroup().getAcademicGroupId()))
                .toList();
    }
}
