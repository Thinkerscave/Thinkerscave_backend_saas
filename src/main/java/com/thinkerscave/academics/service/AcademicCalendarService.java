package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.AcademicCalendarEventRequest;
import com.thinkerscave.academics.dto.response.AcademicCalendarDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicCalendarEventResponse;
import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;

import java.time.LocalDate;
import java.util.List;

public interface AcademicCalendarService {

    AcademicCalendarDashboardResponse getDashboard(
            Long yearId,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to);

    List<AcademicCalendarEventResponse> listEvents(
            Long yearId,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to);

    AcademicCalendarEventResponse getById(Long eventId);

    AcademicCalendarEventResponse create(AcademicCalendarEventRequest request);

    AcademicCalendarEventResponse update(Long eventId, AcademicCalendarEventRequest request);

    AcademicCalendarEventResponse publish(Long eventId);

    AcademicCalendarEventResponse unpublish(Long eventId);

    AcademicCalendarEventResponse deactivate(Long eventId);

    AcademicCalendarEventResponse reactivate(Long eventId);

    List<AcademicCalendarEventResponse> upcoming(Long academicYearId, Integer limit);
}
