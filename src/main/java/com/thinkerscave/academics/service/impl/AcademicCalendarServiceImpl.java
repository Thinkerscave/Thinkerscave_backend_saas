package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.CalendarEventRequest;
import com.thinkerscave.academics.dto.response.CalendarEventResponse;
import com.thinkerscave.academics.entity.AcademicCalendarEvent;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.EventType;
import com.thinkerscave.academics.repository.AcademicCalendarEventRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.service.AcademicCalendarService;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicCalendarServiceImpl implements AcademicCalendarService {

    private final AcademicCalendarEventRepository eventRepository;
    private final AcademicYearRepository academicYearRepository;

    @Override
    @Transactional
    public CalendarEventResponse createEvent(Long academicYearId, CalendarEventRequest request) {
        AcademicYear year = getYear(academicYearId);
        AcademicCalendarEvent event = new AcademicCalendarEvent();
        event.setAcademicYear(year);
        mapRequest(request, event);
        event.setActive(true);
        return toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public CalendarEventResponse updateEvent(Long eventId, CalendarEventRequest request) {
        AcademicCalendarEvent event = getEvent(eventId);
        mapRequest(request, event);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public CalendarEventResponse getById(Long eventId) {
        return toResponse(getEvent(eventId));
    }

    @Override
    public List<CalendarEventResponse> getEventsByYear(Long academicYearId) {
        return eventRepository.findByAcademicYear_AcademicYearIdAndActiveOrderByStartDateAsc(academicYearId, true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CalendarEventResponse> getEventsByMonth(int year, int month) {
        return eventRepository.findByMonthAndYear(month, year)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<CalendarEventResponse> getUpcomingEvents() {
        return eventRepository.findByStartDateGreaterThanEqualAndActiveOrderByStartDateAsc(LocalDate.now(), true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        AcademicCalendarEvent event = getEvent(eventId);
        event.setActive(false);
        eventRepository.save(event);
    }

    // ---- helpers ----

    private AcademicYear getYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private AcademicCalendarEvent getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found: " + id));
    }

    private void mapRequest(CalendarEventRequest request, AcademicCalendarEvent event) {
        event.setTitle(request.getTitle());
        event.setEventType(EventType.valueOf(request.getEventType()));
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setAllDay(request.getAllDay() != null ? request.getAllDay() : true);
        event.setDescription(request.getDescription());
    }

    private CalendarEventResponse toResponse(AcademicCalendarEvent e) {
        return CalendarEventResponse.builder()
                .eventId(e.getEventId())
                .academicYearId(e.getAcademicYear().getAcademicYearId())
                .title(e.getTitle())
                .eventType(e.getEventType().name())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .allDay(e.getAllDay())
                .description(e.getDescription())
                .active(e.getActive())
                .build();
    }
}
