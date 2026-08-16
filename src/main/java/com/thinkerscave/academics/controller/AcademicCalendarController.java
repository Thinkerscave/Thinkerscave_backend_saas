package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicCalendarEventRequest;
import com.thinkerscave.academics.dto.response.AcademicCalendarDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicCalendarEventResponse;
import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;
import com.thinkerscave.academics.service.AcademicCalendarService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Academic Calendar", description = "Academic calendar events, holidays and schedules")
public class AcademicCalendarController {

    private final AcademicCalendarService calendarService;

    @GetMapping("/years/{yearId}/calendar/events/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Academic calendar dashboard for a year")
    public ResponseEntity<ApiResponse<AcademicCalendarDashboardResponse>> dashboard(
            @PathVariable Long yearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CalendarEventType eventType,
            @RequestParam(required = false) CalendarEventStatus status,
            @RequestParam(required = false) CalendarAudienceType audienceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar dashboard",
                calendarService.getDashboard(yearId, q, eventType, status, audienceType, from, to)));
    }

    @GetMapping("/years/{yearId}/calendar/events")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "List calendar events for a year")
    public ResponseEntity<ApiResponse<List<AcademicCalendarEventResponse>>> list(
            @PathVariable Long yearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) CalendarEventType eventType,
            @RequestParam(required = false) CalendarEventStatus status,
            @RequestParam(required = false) CalendarAudienceType audienceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar events",
                calendarService.listEvents(yearId, q, eventType, status, audienceType, from, to)));
    }

    @GetMapping("/calendar/events/{eventId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Get calendar event by ID")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> getById(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success("Calendar event found", calendarService.getById(eventId)));
    }

    @PostMapping("/calendar/events")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> create(
            @Valid @RequestBody AcademicCalendarEventRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Calendar event created", calendarService.create(request)));
    }

    @PutMapping("/calendar/events/{eventId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> update(
            @PathVariable Long eventId, @Valid @RequestBody AcademicCalendarEventRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar event updated", calendarService.update(eventId, request)));
    }

    @PostMapping("/calendar/events/{eventId}/publish")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Publish calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> publish(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success("Calendar event published", calendarService.publish(eventId)));
    }

    @PostMapping("/calendar/events/{eventId}/unpublish")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Unpublish calendar event (back to draft)")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> unpublish(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar event unpublished", calendarService.unpublish(eventId)));
    }

    @PostMapping("/calendar/events/{eventId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate calendar event")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> deactivate(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar event deactivated", calendarService.deactivate(eventId)));
    }

    @PostMapping("/calendar/events/{eventId}/reactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Reactivate inactive calendar event to draft")
    public ResponseEntity<ApiResponse<AcademicCalendarEventResponse>> reactivate(@PathVariable Long eventId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Calendar event reactivated", calendarService.reactivate(eventId)));
    }

    @GetMapping("/calendar/events/upcoming")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER','STUDENT','PARENT')")
    @Operation(summary = "Upcoming calendar events")
    public ResponseEntity<ApiResponse<List<AcademicCalendarEventResponse>>> upcoming(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(
                "Upcoming events", calendarService.upcoming(academicYearId, limit)));
    }
}
