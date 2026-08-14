package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<AcademicClass, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndClassIdNot(Long yearId, String code, Long classId);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long yearId, String name);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndClassIdNot(Long yearId, String name, Long classId);

    Optional<AcademicClass> findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    Optional<AcademicClass> findByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long yearId, String name);

    List<AcademicClass> findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(Long yearId);

    List<AcademicClass> findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(Long yearId);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear y WHERE y.academicYearId = :yearId ORDER BY c.displayOrder ASC")
    List<AcademicClass> findWithYearByAcademicYearIdOrderByDisplayOrderAsc(@Param("yearId") Long yearId);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear y WHERE y.academicYearId = :yearId AND c.active = true ORDER BY c.displayOrder ASC")
    List<AcademicClass> findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(@Param("yearId") Long yearId);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear WHERE c.classId = :classId")
    Optional<AcademicClass> findByIdWithYear(@Param("classId") Long classId);

    /** Legacy alias: class code maps to {@code code}. */
    default Optional<AcademicClass> findByAcademicYear_AcademicYearIdAndClassCodeIgnoreCase(
            Long yearId, String classCode) {
        return findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, classCode);
    }

    /** Legacy alias: class name maps to {@code name}. */
    default Optional<AcademicClass> findByAcademicYear_AcademicYearIdAndClassNameIgnoreCase(
            Long yearId, String className) {
        return findByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, className);
    }
}
