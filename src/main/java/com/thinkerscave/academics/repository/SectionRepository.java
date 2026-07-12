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

    boolean existsByAcademicClass_ClassIdAndSectionName(Long classId, String sectionName);

    boolean existsByAcademicClass_ClassIdAndSectionNameAndSectionIdNot(Long classId, String name, Long sectionId);

    List<AcademicSection> findByAcademicClass_ClassIdAndActiveOrderBySectionNameAsc(Long classId, Boolean active);

    List<AcademicSection> findByAcademicClass_ClassIdOrderBySectionNameAsc(Long classId);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.academicClass.classId = :classId ORDER BY s.sectionName ASC")
    List<AcademicSection> findWithClassByAcademicClass_ClassIdOrderBySectionNameAsc(@Param("classId") Long classId);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.academicClass.classId = :classId AND s.active = :active ORDER BY s.sectionName ASC")
    List<AcademicSection> findWithClassByAcademicClass_ClassIdAndActiveOrderBySectionNameAsc(@Param("classId") Long classId, @Param("active") Boolean active);

    @Query("SELECT s FROM AcademicSection s JOIN FETCH s.academicClass WHERE s.sectionId = :sectionId")
    Optional<AcademicSection> findByIdWithClass(@Param("sectionId") Long sectionId);

    Optional<AcademicSection> findByAcademicClass_ClassIdAndSectionNameIgnoreCase(Long classId, String sectionName);

    boolean existsByAcademicClass_ClassId(Long classId);
}
