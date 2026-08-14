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

    boolean existsByAcademicClass_ClassIdAndCodeIgnoreCase(Long classId, String code);

    boolean existsByAcademicClass_ClassIdAndCodeIgnoreCaseAndSectionIdNot(Long classId, String code, Long sectionId);

    boolean existsByAcademicClass_ClassIdAndNameIgnoreCase(Long classId, String name);

    boolean existsByAcademicClass_ClassIdAndNameIgnoreCaseAndSectionIdNot(Long classId, String name, Long sectionId);

    List<AcademicSection> findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(Long classId);

    List<AcademicSection> findByAcademicClass_ClassIdOrderByDisplayOrderAsc(Long classId);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.academicClass.classId = :classId ORDER BY s.displayOrder ASC")
    List<AcademicSection> findWithClassByAcademicClass_ClassIdOrderByDisplayOrderAsc(@Param("classId") Long classId);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.academicClass.classId = :classId AND s.active = true ORDER BY s.displayOrder ASC")
    List<AcademicSection> findWithClassByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(
            @Param("classId") Long classId);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.sectionId = :sectionId")
    Optional<AcademicSection> findByIdWithClass(@Param("sectionId") Long sectionId);

    Optional<AcademicSection> findByAcademicClass_ClassIdAndCodeIgnoreCase(Long classId, String code);

    Optional<AcademicSection> findByAcademicClass_ClassIdAndNameIgnoreCase(Long classId, String name);

    /** Legacy alias: section name maps to {@code name}. */
    default Optional<AcademicSection> findByAcademicClass_ClassIdAndSectionNameIgnoreCase(
            Long classId, String sectionName) {
        return findByAcademicClass_ClassIdAndNameIgnoreCase(classId, sectionName);
    }

    boolean existsByAcademicClass_ClassId(Long classId);
}
