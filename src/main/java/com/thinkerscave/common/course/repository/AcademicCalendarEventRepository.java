package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicCalendarEvent;
import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.orgm.domain.Organisation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicCalendarEventRepository extends JpaRepository<AcademicCalendarEvent, Long> {

    @EntityGraph(attributePaths = { "organization", "academicYear" })
    List<AcademicCalendarEvent> findByOrganizationAndAcademicYearAndIsActiveTrueOrderByStartDateAscEndDateAsc(
            Organisation organization,
            AcademicYear academicYear);

    @EntityGraph(attributePaths = { "organization", "academicYear" })
    Optional<AcademicCalendarEvent> findByEventIdAndOrganization(Long eventId, Organisation organization);
}