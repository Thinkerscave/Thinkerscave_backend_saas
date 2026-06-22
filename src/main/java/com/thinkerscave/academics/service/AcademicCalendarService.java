package com.thinkerscave.academics.service;

import com.thinkerscave.academics.dto.request.CalendarEventRequest;
import com.thinkerscave.academics.dto.response.CalendarEventResponse;

import java.util.List;

public interface AcademicCalendarService {

    CalendarEventResponse createEvent(Long academicYearId, CalendarEventRequest request);

    CalendarEventResponse updateEvent(Long eventId, CalendarEventRequest request);

    CalendarEventResponse getById(Long eventId);

    List<CalendarEventResponse> getEventsByYear(Long academicYearId);

    List<CalendarEventResponse> getEventsByMonth(int year, int month);

    List<CalendarEventResponse> getUpcomingEvents();

    void deleteEvent(Long eventId);
}
