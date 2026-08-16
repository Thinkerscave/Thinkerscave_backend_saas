package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndAcademicYearIdNot(String name, Long id);

    Optional<AcademicYear> findByNameIgnoreCase(String name);

    Optional<AcademicYear> findByStatus(AcademicYearStatus status);

    Optional<AcademicYear> findByStatusAndActiveTrue(AcademicYearStatus status);

    List<AcademicYear> findByStatusInOrderByStartDateDesc(List<AcademicYearStatus> statuses);

    List<AcademicYear> findByActiveTrueOrderByStartDateDesc();

    List<AcademicYear> findByActiveOrderByStartDateDesc(boolean active);

    Page<AcademicYear> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<AcademicYear> findAllByOrderByStartDateDesc(Pageable pageable);

    Page<AcademicYear> findByStatusOrderByStartDateDesc(AcademicYearStatus status, Pageable pageable);

    Page<AcademicYear> findByNameContainingIgnoreCaseAndStatusOrderByStartDateDesc(
            String name, AcademicYearStatus status, Pageable pageable);

    @Query("""
            SELECT COUNT(y) > 0 FROM AcademicYear y
            WHERE y.active = true
              AND y.status NOT IN (
                    com.thinkerscave.academics.enums.AcademicYearStatus.COMPLETED,
                    com.thinkerscave.academics.enums.AcademicYearStatus.ARCHIVED
              )
              AND y.academicYearId <> COALESCE(:excludeId, -1L)
              AND y.startDate <= :endDate
              AND y.endDate >= :startDate
            """)
    boolean existsOverlappingActiveRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AcademicYear y SET y.status = com.thinkerscave.academics.enums.AcademicYearStatus.COMPLETED, "
            + "y.active = false "
            + "WHERE y.status = com.thinkerscave.academics.enums.AcademicYearStatus.CURRENT")
    void clearCurrentYearStatus();

    @Query("SELECT y FROM AcademicYear y WHERE UPPER(y.name) = UPPER(:yearCode)")
    Optional<AcademicYear> findByYearCode(@Param("yearCode") String yearCode);

    @Query("SELECT y FROM AcademicYear y WHERE y.status = com.thinkerscave.academics.enums.AcademicYearStatus.CURRENT")
    Optional<AcademicYear> findByCurrentYearTrue();
}
