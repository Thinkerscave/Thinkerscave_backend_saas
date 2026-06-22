package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassRepository extends JpaRepository<AcademicClass, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndClassCode(Long yearId, String classCode);

    boolean existsByAcademicYear_AcademicYearIdAndClassCodeAndClassIdNot(Long yearId, String classCode, Long classId);

    List<AcademicClass> findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(Long yearId);

    List<AcademicClass> findByAcademicYear_AcademicYearIdAndActiveOrderByDisplayOrderAsc(Long yearId, Boolean active);

    boolean existsByClassIdAndActiveFalse(Long classId);
}
