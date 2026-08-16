package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndSubjectIdNot(Long yearId, String code, Long subjectId);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long yearId, String name);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndSubjectIdNot(Long yearId, String name, Long subjectId);

    List<Subject> findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(Long yearId);

    List<Subject> findByAcademicYear_AcademicYearIdOrderByNameAsc(Long yearId);

    Optional<Subject> findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    @Query("SELECT s FROM Subject s JOIN FETCH s.academicYear WHERE s.subjectId = :subjectId")
    Optional<Subject> findByIdWithYear(@Param("subjectId") Long subjectId);

    @Query("SELECT s FROM Subject s JOIN FETCH s.academicYear y WHERE y.academicYearId = :yearId ORDER BY s.name ASC")
    List<Subject> findWithYearByAcademicYearIdOrderByNameAsc(@Param("yearId") Long yearId);

    long countByAcademicYear_AcademicYearId(Long yearId);

    long countByAcademicYear_AcademicYearIdAndActiveTrue(Long yearId);

    List<Subject> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code);
}
