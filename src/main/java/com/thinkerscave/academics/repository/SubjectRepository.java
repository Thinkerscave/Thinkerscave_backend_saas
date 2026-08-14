package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndSubjectIdNot(Long yearId, String code, Long subjectId);

    List<Subject> findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(Long yearId);

    List<Subject> findByAcademicYear_AcademicYearIdOrderByNameAsc(Long yearId);

    Optional<Subject> findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    List<Subject> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
