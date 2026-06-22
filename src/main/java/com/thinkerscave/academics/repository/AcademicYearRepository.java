package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    boolean existsByYearCode(String yearCode);

    boolean existsByYearCodeAndAcademicYearIdNot(String yearCode, Long id);

    Optional<AcademicYear> findByCurrentYearTrue();

    List<AcademicYear> findByActiveOrderByStartDateDesc(Boolean active);

    Page<AcademicYear> findByYearCodeContainingIgnoreCaseOrYearNameContainingIgnoreCase(
            String code, String name, Pageable pageable);

    Page<AcademicYear> findAllByOrderByStartDateDesc(Pageable pageable);

    @Modifying
    @Query("UPDATE AcademicYear y SET y.currentYear = false WHERE y.currentYear = true")
    void clearCurrentYear();
}
