package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<AcademicSection, Long> {

    boolean existsByAcademicClass_ClassIdAndSectionName(Long classId, String sectionName);

    boolean existsByAcademicClass_ClassIdAndSectionNameAndSectionIdNot(Long classId, String name, Long sectionId);

    List<AcademicSection> findByAcademicClass_ClassIdAndActiveOrderBySectionNameAsc(Long classId, Boolean active);

    List<AcademicSection> findByAcademicClass_ClassIdOrderBySectionNameAsc(Long classId);

    boolean existsByAcademicClass_ClassId(Long classId);
}
