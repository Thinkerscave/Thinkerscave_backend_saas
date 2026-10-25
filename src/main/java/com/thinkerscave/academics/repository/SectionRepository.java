package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<AcademicSection, Long> {

    boolean existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(Long academicGroupId, String code);

    boolean existsByAcademicGroup_AcademicGroupIdAndCodeIgnoreCaseAndSectionIdNot(Long academicGroupId, String code, Long sectionId);

    boolean existsByAcademicGroup_AcademicGroupIdAndNameIgnoreCase(Long academicGroupId, String name);

    boolean existsByAcademicGroup_AcademicGroupIdAndNameIgnoreCaseAndSectionIdNot(Long academicGroupId, String name, Long sectionId);

        List<AcademicSection> findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(Long academicGroupId);

        List<AcademicSection> findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(Long academicGroupId);

        @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicGroup WHERE s.academicGroup.academicGroupId = :academicGroupId ORDER BY s.displayOrder ASC")
        List<AcademicSection> findWithAcademicGroupByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(@Param("academicGroupId") Long academicGroupId);

        @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicGroup WHERE s.academicGroup.academicGroupId = :academicGroupId AND s.active = true ORDER BY s.displayOrder ASC")
        List<AcademicSection> findWithAcademicGroupByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("academicGroupId") Long academicGroupId);

    @Query("""
            SELECT s FROM AcademicSection s
            JOIN FETCH s.academicGroup A
            LEFT JOIN FETCH s.defaultResource
            WHERE A.academicYear.academicYearId = :yearId
              AND A.active = true
              AND s.active = true
            ORDER BY A.displayOrder ASC, s.displayOrder ASC, s.name ASC
            """)
    List<AcademicSection> findActiveForTimetableByYear(@Param("yearId") Long yearId);

        @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicGroup A JOIN FETCH A.academicYear WHERE s.sectionId = :sectionId")
        Optional<AcademicSection> findByIdWithAcademicGroup(@Param("sectionId") Long sectionId);

        Optional<AcademicSection> findByAcademicGroup_AcademicGroupIdAndCodeIgnoreCase(Long academicGroupId, String code);

        Optional<AcademicSection> findByAcademicGroup_AcademicGroupIdAndNameIgnoreCase(Long academicGroupId, String name);

        @Query("SELECT s FROM AcademicSection s WHERE s.academicGroup.academicGroupId = :classId AND LOWER(s.name) = LOWER(:name)")
        Optional<AcademicSection> findByAcademicClass_ClassIdAndNameIgnoreCase(@Param("classId") Long classId,
                                                                                @Param("name") String name);

        @Query("SELECT s FROM AcademicSection s WHERE s.academicGroup.academicGroupId = :classId AND LOWER(s.name) = LOWER(:name)")
        Optional<AcademicSection> findByAcademicClass_ClassIdAndSectionNameIgnoreCase(@Param("classId") Long classId,
                                                                                        @Param("name") String name);

    /** Legacy alias: section name maps to {@code name}. */
        default Optional<AcademicSection> findByAcademicGroup_AcademicGroupIdAndSectionNameIgnoreCase(
            Long academicGroupId, String sectionName) {
        return findByAcademicGroup_AcademicGroupIdAndNameIgnoreCase(academicGroupId, sectionName);
    }

    boolean existsByAcademicGroup_AcademicGroupId(Long academicGroupId);

    long countByAcademicGroup_AcademicYear_AcademicYearId(Long yearId);

    long countByAcademicGroup_AcademicYear_AcademicYearIdAndActiveTrue(Long yearId);

    long countByAcademicGroup_AcademicGroupId(Long academicGroupId);

    long countByAcademicGroup_AcademicGroupIdAndActiveTrue(Long academicGroupId);

        @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicGroup A WHERE A.academicYear.academicYearId = :yearId")
    List<AcademicSection> findWithAcademicGroupByYearId(@Param("yearId") Long yearId);
}
