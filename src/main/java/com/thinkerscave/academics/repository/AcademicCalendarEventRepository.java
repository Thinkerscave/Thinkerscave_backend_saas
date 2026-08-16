package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicCalendarEvent;
import com.thinkerscave.academics.enums.CalendarEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Long> {

    @Query("""
            SELECT e FROM AcademicCalendarEvent e
            JOIN FETCH e.academicYear
            WHERE e.eventId = :eventId
            """)
    Optional<AcademicCalendarEvent> findByIdWithYear(@Param("eventId") Long eventId);

    @Query("""
            SELECT e FROM AcademicCalendarEvent e
            JOIN FETCH e.academicYear
            WHERE e.academicYear.academicYearId = :yearId
            ORDER BY e.startDate ASC, e.title ASC
            """)
    List<AcademicCalendarEvent> findWithYearByAcademicYearId(@Param("yearId") Long yearId);

    long countByAcademicYear_AcademicYearId(Long yearId);

    long countByAcademicYear_AcademicYearIdAndEventType(Long yearId, CalendarEventType eventType);

    @Query("""
            SELECT e FROM AcademicCalendarEvent e
            JOIN FETCH e.academicYear
            WHERE e.academicYear.academicYearId = :yearId
              AND e.endDate >= :fromDate
            ORDER BY e.startDate ASC, e.title ASC
            """)
    List<AcademicCalendarEvent> findUpcomingByYear(
            @Param("yearId") Long yearId,
            @Param("fromDate") LocalDate fromDate);
}
