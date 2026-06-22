package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicCalendarEvent;
import com.thinkerscave.academics.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Long> {

    @Query("SELECT e FROM AcademicCalendarEvent e WHERE e.active = true " +
           "AND FUNCTION('MONTH', e.startDate) = :month AND FUNCTION('YEAR', e.startDate) = :year " +
           "ORDER BY e.startDate ASC")
    List<AcademicCalendarEvent> findByMonthAndYear(@Param("month") int month, @Param("year") int year);

    List<AcademicCalendarEvent> findByStartDateGreaterThanEqualAndActiveOrderByStartDateAsc(
            LocalDate from, Boolean active);

    List<AcademicCalendarEvent> findByAcademicYear_AcademicYearIdAndActiveOrderByStartDateAsc(
            Long yearId, Boolean active);
}
