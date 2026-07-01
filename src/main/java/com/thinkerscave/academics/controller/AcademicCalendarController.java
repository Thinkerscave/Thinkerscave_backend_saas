package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.CalendarEventRequest;
import com.thinkerscave.academics.dto.response.CalendarEventResponse;
import com.thinkerscave.academics.service.AcademicCalendarService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics/calendar")
@RequiredArgsConstructor
@Tag(name = "Academic Calendar", description = "Manage academic calendar events")
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    @PostMapping("/years/{yearId}/events")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create calendar event")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> create(
            @PathVariable Long yearId, @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Event created", calendarService.createEvent(yearId, request)));
    }

    @PutMapping("/events/{eventId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update calendar event")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> update(
            @PathVariable Long eventId, @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Event updated", calendarService.updateEvent(eventId, request)));
    }

    @GetMapping("/events/{eventId}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<ApiResponse<CalendarEventResponse>> getById(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success("Event found", calendarService.getById(eventId)));
    }

    @GetMapping("/years/{yearId}/events")
    @Operation(summary = "Get events by academic year")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getByYear(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Events retrieved", calendarService.getEventsByYear(yearId)));
    }

    @GetMapping("/events/month")
    @Operation(summary = "Get events by month")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getByMonth(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.success("Events retrieved", calendarService.getEventsByMonth(year, month)));
    }

    @GetMapping("/events/upcoming")
    @Operation(summary = "Get upcoming events")
    public ResponseEntity<ApiResponse<List<CalendarEventResponse>>> getUpcoming() {
        return ResponseEntity.ok(ApiResponse.success("Upcoming events retrieved", calendarService.getUpcomingEvents()));
    }

    @DeleteMapping("/events/{eventId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Delete calendar event")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long eventId) {
        calendarService.deleteEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }
}
