package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.FixedActivityPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FixedActivityPeriodRepository extends JpaRepository<FixedActivityPeriod, Long> {

    @Query("""
            SELECT COUNT(p) FROM FixedActivityPeriod p
            WHERE p.timetablePeriod.schedulePattern.schedulePatternId = :patternId
            """)
    long countBySchedulePatternId(@Param("patternId") Long patternId);
}
