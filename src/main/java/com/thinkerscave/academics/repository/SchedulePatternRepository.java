package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SchedulePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulePatternRepository extends JpaRepository<SchedulePattern, Long> {

    List<SchedulePattern> findByAcademicYear_AcademicYearIdOrderByNameAsc(Long academicYearId);

    List<SchedulePattern> findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(Long academicYearId);

    Optional<SchedulePattern> findByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);

    boolean existsByAcademicYear_AcademicYearIdAndNameIgnoreCaseAndSchedulePatternIdNot(
            Long academicYearId, String name, Long schedulePatternId);

    @Query("""
            SELECT p FROM SchedulePattern p
            JOIN FETCH p.academicYear
            WHERE p.schedulePatternId = :id
            """)
    Optional<SchedulePattern> findByIdWithYear(@Param("id") Long id);
}
