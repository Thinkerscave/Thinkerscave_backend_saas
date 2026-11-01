package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicGroup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Long> {

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    boolean existsByAcademicYear_AcademicYearIdAndCodeIgnoreCaseAndAcademicGroupIdNot(Long yearId, String code, Long academicGroupId);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long yearId, String name);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndAcademicGroupIdNot(Long yearId, String name, Long academicGroupId);

    Optional<AcademicGroup> findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(Long yearId, String code);

    Optional<AcademicGroup> findByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long yearId, String name);

    List<AcademicGroup> findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(Long yearId);

    List<AcademicGroup> findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(Long yearId);

    @Query("SELECT A FROM AcademicGroup A JOIN FETCH A.academicYear y WHERE y.academicYearId = :yearId ORDER BY A.displayOrder ASC")
    List<AcademicGroup> findWithYearByAcademicYearIdOrderByDisplayOrderAsc(@Param("yearId") Long yearId);

    @Query("SELECT A FROM AcademicGroup A JOIN FETCH A.academicYear y WHERE y.academicYearId = :yearId AND A.active = true ORDER BY A.displayOrder ASC")
    List<AcademicGroup> findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(@Param("yearId") Long yearId);

    @Query("""
            SELECT A FROM AcademicGroup A
            WHERE LOWER(A.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(A.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY A.displayOrder ASC
            """)
    List<AcademicGroup> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT A FROM AcademicGroup A JOIN FETCH A.academicYear WHERE A.academicGroupId = :academicGroupId")
    Optional<AcademicGroup> findByIdWithYear(@Param("academicGroupId") Long academicGroupId);

    /** Legacy alias: class code maps to {@code code}. */
    default Optional<AcademicGroup> findByAcademicYear_AcademicYearIdAndAcademicGroupCodeIgnoreCase(
            Long yearId, String academicGroupCode) {
        return findByAcademicYear_AcademicYearIdAndCodeIgnoreCase(yearId, academicGroupCode);
    }

    /** Legacy alias: class name maps to {@code name}. */
    default Optional<AcademicGroup> findByAcademicYear_AcademicYearIdAndAcademicGroupNameIgnoreCase(
            Long yearId, String academicGroupName) {
        return findByAcademicYear_AcademicYearIdAndNameIgnoreCase(yearId, academicGroupName);
    }

    long countByAcademicYear_AcademicYearId(Long yearId);

    long countByAcademicYear_AcademicYearIdAndActiveTrue(Long yearId);
}
