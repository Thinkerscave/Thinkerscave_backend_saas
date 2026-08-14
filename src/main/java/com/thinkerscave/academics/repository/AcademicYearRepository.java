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

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndAcademicYearIdNot(String name, Long id);

    Optional<AcademicYear> findByNameIgnoreCase(String name);

    Optional<AcademicYear> findByStatus(AcademicYearStatus status);

    List<AcademicYear> findByStatusInOrderByStartDateDesc(List<AcademicYearStatus> statuses);

    Page<AcademicYear> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<AcademicYear> findAllByOrderByStartDateDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE AcademicYear y SET y.status = com.thinkerscave.academics.enums.AcademicYearStatus.COMPLETED "
            + "WHERE y.status = com.thinkerscave.academics.enums.AcademicYearStatus.CURRENT")
    void clearCurrentYearStatus();

    /** Legacy alias: year code maps to {@code name}. */
    @Query("SELECT y FROM AcademicYear y WHERE UPPER(y.name) = UPPER(:yearCode)")
    Optional<AcademicYear> findByYearCode(@Param("yearCode") String yearCode);

    /** Legacy alias: current year is {@code status = CURRENT}. */
    @Query("SELECT y FROM AcademicYear y WHERE y.status = com.thinkerscave.academics.enums.AcademicYearStatus.CURRENT")
    Optional<AcademicYear> findByCurrentYearTrue();

    List<AcademicYear> findByActiveOrderByStartDateDesc(boolean active);
}
