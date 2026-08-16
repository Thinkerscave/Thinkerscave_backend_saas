package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableWorkingDay;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableWorkingDayRepository extends JpaRepository<TimetableWorkingDay, Long> {

    List<TimetableWorkingDay> findByTimetableConfiguration_TimetableConfigurationId(Long configurationId);

    Optional<TimetableWorkingDay> findByTimetableConfiguration_TimetableConfigurationIdAndDayOfWeek(
            Long configurationId, DayOfWeek dayOfWeek);
}
