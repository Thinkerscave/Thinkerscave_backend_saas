package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetablePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, Long> {

    List<TimetablePeriod> findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(
            Long configurationId);
}
