package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetablePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, Long> {

    List<TimetablePeriod> findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(
            Long configurationId);

    List<TimetablePeriod> findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(Long patternId);

    @Query("""
            SELECT p FROM TimetablePeriod p
            JOIN FETCH p.schedulePattern sp
            WHERE sp.academicYear.academicYearId = :yearId
            ORDER BY sp.name ASC, p.periodNumber ASC
            """)
    List<TimetablePeriod> findAllSlotsForYear(@Param("yearId") Long yearId);

    void deleteBySchedulePattern_SchedulePatternId(Long patternId);
}
