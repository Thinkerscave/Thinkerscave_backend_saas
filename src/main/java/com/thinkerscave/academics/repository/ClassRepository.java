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

    boolean existsByAcademicYear_AcademicYearIdAndClassCode(Long yearId, String classCode);

    boolean existsByAcademicYear_AcademicYearIdAndClassCodeAndClassIdNot(Long yearId, String classCode, Long classId);

    List<AcademicClass> findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(Long yearId);

    List<AcademicClass> findByAcademicYear_AcademicYearIdAndActiveOrderByDisplayOrderAsc(Long yearId, Boolean active);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear y WHERE y.academicYearId = :yearId ORDER BY c.displayOrder ASC")
    List<AcademicClass> findWithYearByAcademicYearIdOrderByDisplayOrderAsc(@Param("yearId") Long yearId);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear y WHERE y.academicYearId = :yearId AND c.active = :active ORDER BY c.displayOrder ASC")
    List<AcademicClass> findWithYearByAcademicYearIdAndActiveOrderByDisplayOrderAsc(@Param("yearId") Long yearId, @Param("active") Boolean active);

    @Query("SELECT c FROM AcademicClass c JOIN FETCH c.academicYear WHERE c.classId = :classId")
    Optional<AcademicClass> findByIdWithYear(@Param("classId") Long classId);

    boolean existsByClassIdAndActiveFalse(Long classId);
}
