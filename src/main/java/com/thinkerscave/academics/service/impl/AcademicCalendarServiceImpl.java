package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicCalendarEventRequest;
import com.thinkerscave.academics.dto.response.AcademicCalendarDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicCalendarEventResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.CalendarAudienceType;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;
import com.thinkerscave.academics.repository.AcademicCalendarEventRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicCalendarService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.student.entity.Parent;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentParent;
import com.thinkerscave.student.repository.ParentRepository;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentParentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicCalendarServiceImpl implements AcademicCalendarService {

    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );
    private static final int DEFAULT_UPCOMING_LIMIT = 6;
    private static final int MAX_UPCOMING_LIMIT = 8;

    private final AcademicCalendarEventRepository eventRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final TeacherAllocationTeacherRepository tatRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ParentRepository parentRepository;
    private final StudentParentRepository studentParentRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public AcademicCalendarDashboardResponse getDashboard(
            Long yearId,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicYear year = requireYear(yearId);
        List<AcademicCalendarEventResponse> events = filterAndMap(yearId, q, eventType, status, audienceType, from, to);
        List<AcademicCalendarEventResponse> upcoming = buildUpcoming(yearId, DEFAULT_UPCOMING_LIMIT);

        return AcademicCalendarDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .name(year.getName())
                .status(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .eventCount(events.size())
                .holidayCount(countType(events, CalendarEventType.HOLIDAY))
                .examinationCount(countType(events, CalendarEventType.EXAMINATION))
                .schoolEventCount(countType(events, CalendarEventType.SCHOOL_EVENT))
                .academicEventCount(countType(events, CalendarEventType.ACADEMIC_EVENT))
                .otherCount(countType(events, CalendarEventType.OTHER))
                .upcoming(upcoming)
                .events(events)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicCalendarEventResponse> listEvents(
            Long yearId,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CALENDAR);
        requireYear(yearId);
        return filterAndMap(yearId, q, eventType, status, audienceType, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicCalendarEventResponse getById(Long eventId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        initializeMappings(event);
        if (!isVisibleToCurrentUser(event)) {
            throw new ResourceNotFoundException("Calendar event not found: " + eventId);
        }
        return toResponse(event);
    }

    @Override
    public AcademicCalendarEventResponse create(AcademicCalendarEventRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicYear year = requireYear(request.getAcademicYearId());
        assertYearMutable(year);
        validateRequest(request, year);

        AcademicCalendarEvent event = new AcademicCalendarEvent();
        event.setAcademicYear(year);
        applyRequestFields(event, request);

        if (Boolean.TRUE.equals(request.getPublish())) {
            markPublished(event);
        } else {
            event.setStatus(CalendarEventStatus.DRAFT);
            event.setPublishedBy(null);
            event.setPublishedOn(null);
        }

        replaceAudienceMappings(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public AcademicCalendarEventResponse update(Long eventId, AcademicCalendarEventRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        assertYearMutable(event.getAcademicYear());
        if (!Objects.equals(event.getAcademicYear().getAcademicYearId(), request.getAcademicYearId())) {
            throw new BusinessException("Academic year cannot be changed for an existing event");
        }
        validateRequest(request, event.getAcademicYear());

        applyRequestFields(event, request);
        replaceAudienceMappings(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public AcademicCalendarEventResponse publish(Long eventId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        assertYearMutable(event.getAcademicYear());
        if (event.getStatus() == CalendarEventStatus.INACTIVE) {
            throw new BusinessException("Reactivate the event before publishing");
        }
        markPublished(event);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public AcademicCalendarEventResponse unpublish(Long eventId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        assertYearMutable(event.getAcademicYear());
        if (event.getStatus() != CalendarEventStatus.PUBLISHED) {
            throw new BusinessException("Only published events can be unpublished");
        }
        event.setStatus(CalendarEventStatus.DRAFT);
        event.setPublishedBy(null);
        event.setPublishedOn(null);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public AcademicCalendarEventResponse deactivate(Long eventId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        assertYearMutable(event.getAcademicYear());
        event.setStatus(CalendarEventStatus.INACTIVE);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public AcademicCalendarEventResponse reactivate(Long eventId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AcademicCalendarEvent event = requireEvent(eventId);
        assertYearMutable(event.getAcademicYear());
        if (event.getStatus() != CalendarEventStatus.INACTIVE) {
            throw new BusinessException("Only inactive events can be reactivated");
        }
        event.setStatus(CalendarEventStatus.DRAFT);
        event.setPublishedBy(null);
        event.setPublishedOn(null);
        return toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicCalendarEventResponse> upcoming(Long academicYearId, Integer limit) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_CALENDAR);
        Long yearId = academicYearId;
        if (yearId == null) {
            yearId = academicYearRepository.findByCurrentYearTrue()
                    .map(AcademicYear::getAcademicYearId)
                    .orElseThrow(() -> new BusinessException("No current academic year found"));
        } else {
            requireYear(yearId);
        }
        int capped = limit == null ? DEFAULT_UPCOMING_LIMIT : Math.min(Math.max(limit, 1), MAX_UPCOMING_LIMIT);
        return buildUpcoming(yearId, capped);
    }

    // ─── filtering / visibility ───────────────────────────────────────────

    private List<AcademicCalendarEventResponse> filterAndMap(
            Long yearId,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to) {
        boolean manager = accessGuard.canManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AudienceScope scope = manager ? AudienceScope.all() : resolveAudienceScope(yearId);

        return eventRepository.findWithYearByAcademicYearId(yearId).stream()
                .peek(this::initializeMappings)
                .filter(e -> matchesFilters(e, q, eventType, status, audienceType, from, to, manager))
                .filter(e -> manager || isVisibleForScope(e, scope))
                .map(this::toResponse)
                .toList();
    }

    private List<AcademicCalendarEventResponse> buildUpcoming(Long yearId, int limit) {
        boolean manager = accessGuard.canManage(AcademicsAccessGuard.RESOURCE_CALENDAR);
        AudienceScope scope = manager ? AudienceScope.all() : resolveAudienceScope(yearId);
        LocalDate today = LocalDate.now();

        return eventRepository.findUpcomingByYear(yearId, today).stream()
                .peek(this::initializeMappings)
                .filter(e -> {
                    if (manager) {
                        return e.getStatus() == CalendarEventStatus.PUBLISHED
                                || e.getStatus() == CalendarEventStatus.DRAFT;
                    }
                    return e.getStatus() == CalendarEventStatus.PUBLISHED && isVisibleForScope(e, scope);
                })
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    private boolean matchesFilters(
            AcademicCalendarEvent e,
            String q,
            CalendarEventType eventType,
            CalendarEventStatus status,
            CalendarAudienceType audienceType,
            LocalDate from,
            LocalDate to,
            boolean manager) {
        if (!manager && e.getStatus() != CalendarEventStatus.PUBLISHED) {
            return false;
        }
        if (status != null && e.getStatus() != status) {
            return false;
        }
        if (eventType != null && e.getEventType() != eventType) {
            return false;
        }
        if (audienceType != null && e.getAudienceType() != audienceType) {
            return false;
        }
        if (from != null && e.getEndDate().isBefore(from)) {
            return false;
        }
        if (to != null && e.getStartDate().isAfter(to)) {
            return false;
        }
        return matchesQuery(e, q);
    }

    private boolean matchesQuery(AcademicCalendarEvent e, String q) {
        if (!StringUtils.hasText(q)) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        return (e.getTitle() != null && e.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                || (e.getDescription() != null && e.getDescription().toLowerCase(Locale.ROOT).contains(needle))
                || (e.getLocation() != null && e.getLocation().toLowerCase(Locale.ROOT).contains(needle));
    }

    private boolean isVisibleToCurrentUser(AcademicCalendarEvent event) {
        if (accessGuard.canManage(AcademicsAccessGuard.RESOURCE_CALENDAR)) {
            return true;
        }
        if (event.getStatus() != CalendarEventStatus.PUBLISHED) {
            return false;
        }
        return isVisibleForScope(event, resolveAudienceScope(event.getAcademicYear().getAcademicYearId()));
    }

    private boolean isVisibleForScope(AcademicCalendarEvent event, AudienceScope scope) {
        if (scope.viewAll()) {
            return true;
        }
        return switch (event.getAudienceType()) {
            case EVERYONE -> true;
            case CLASS -> event.getClasses().stream()
                    .map(c -> c.getAcademicClass().getClassId())
                    .anyMatch(scope.classIds()::contains);
            case SECTION -> event.getSections().stream()
                    .map(s -> s.getSection().getSectionId())
                    .anyMatch(scope.sectionIds()::contains);
        };
    }

    /**
     * Resolves class/section associations for TEACHER (allocations), STUDENT (enrollment),
     * and PARENT (linked children enrollments). Elevated roles use canManage / viewAll.
     */
    private AudienceScope resolveAudienceScope(Long yearId) {
        if (accessGuard.hasElevatedRole()) {
            return AudienceScope.all();
        }
        Long userId = accessGuard.currentUserIdOrNull();
        if (userId == null) {
            return AudienceScope.empty();
        }

        Set<Long> classIds = new HashSet<>();
        Set<Long> sectionIds = new HashSet<>();

        staffRepository.findByUser_Id(userId).ifPresent(staff -> addTeacherScope(staff, yearId, classIds, sectionIds));
        studentRepository.findByUser_Id(userId).ifPresent(student -> addStudentScope(student, classIds, sectionIds));
        parentRepository.findByUser_Id(userId).ifPresent(parent -> addParentScope(parent, classIds, sectionIds));

        return new AudienceScope(false, classIds, sectionIds);
    }

    private void addTeacherScope(Staff staff, Long yearId, Set<Long> classIds, Set<Long> sectionIds) {
        tatRepository.findActiveByStaffAndYear(staff.getStaffId(), yearId).forEach(tat -> {
            AcademicSection section = tat.getTeacherAllocation().getSection();
            sectionIds.add(section.getSectionId());
            classIds.add(section.getAcademicClass().getClassId());
        });
    }

    private void addStudentScope(Student student, Set<Long> classIds, Set<Long> sectionIds) {
        enrollmentRepository.findActiveWithClassByStudentId(student.getStudentId()).ifPresent(e -> {
            if (e.getClassEntity() != null) {
                classIds.add(e.getClassEntity().getClassId());
            }
            if (e.getSection() != null) {
                sectionIds.add(e.getSection().getSectionId());
            }
        });
    }

    private void addParentScope(Parent parent, Set<Long> classIds, Set<Long> sectionIds) {
        List<StudentParent> links = studentParentRepository.findByParent_ParentIdAndActiveTrue(parent.getParentId());
        for (StudentParent link : links) {
            if (link.getStudent() == null) {
                continue;
            }
            addStudentScope(link.getStudent(), classIds, sectionIds);
        }
    }

    // ─── validation / mapping ─────────────────────────────────────────────

    private void validateRequest(AcademicCalendarEventRequest request, AcademicYear year) {
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getTitle().trim())) {
            throw new BusinessException("Title is required");
        }
        if (request.getTitle().trim().length() > 200) {
            throw new BusinessException("Title cannot exceed 200 characters");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("End date must be on or after start date");
        }
        if (request.getStartDate().isBefore(year.getStartDate()) || request.getEndDate().isAfter(year.getEndDate())) {
            throw new BusinessException("Event dates must fall within the academic year");
        }

        boolean allDay = Boolean.TRUE.equals(request.getAllDay());
        if (allDay) {
            request.setStartTime(null);
            request.setEndTime(null);
        } else {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new BusinessException("Start and end times are required when the event is not all-day");
            }
            if (!request.getEndTime().isAfter(request.getStartTime())) {
                throw new BusinessException("End time must be after start time");
            }
        }

        CalendarAudienceType audience = request.getAudienceType();
        List<Long> classIds = request.getClassIds() == null ? List.of() : request.getClassIds().stream()
                .filter(Objects::nonNull).distinct().toList();
        List<Long> sectionIds = request.getSectionIds() == null ? List.of() : request.getSectionIds().stream()
                .filter(Objects::nonNull).distinct().toList();

        switch (audience) {
            case EVERYONE -> {
                request.setClassIds(Collections.emptyList());
                request.setSectionIds(Collections.emptyList());
            }
            case CLASS -> {
                if (classIds.isEmpty()) {
                    throw new BusinessException("At least one class is required for CLASS audience");
                }
                for (Long classId : classIds) {
                    AcademicClass cls = classRepository.findByIdWithYear(classId)
                            .orElseThrow(() -> new BusinessException("Class not found: " + classId));
                    if (!Objects.equals(cls.getAcademicYear().getAcademicYearId(), year.getAcademicYearId())) {
                        throw new BusinessException("Class must belong to the same academic year");
                    }
                }
                request.setClassIds(classIds);
                request.setSectionIds(Collections.emptyList());
            }
            case SECTION -> {
                if (sectionIds.isEmpty()) {
                    throw new BusinessException("At least one section is required for SECTION audience");
                }
                for (Long sectionId : sectionIds) {
                    AcademicSection section = sectionRepository.findByIdWithClass(sectionId)
                            .orElseThrow(() -> new BusinessException("Section not found: " + sectionId));
                    Long sectionYearId = section.getAcademicClass().getAcademicYear().getAcademicYearId();
                    if (!Objects.equals(sectionYearId, year.getAcademicYearId())) {
                        throw new BusinessException("Section must belong to the same academic year");
                    }
                }
                request.setSectionIds(sectionIds);
                request.setClassIds(Collections.emptyList());
            }
        }
    }

    private void applyRequestFields(AcademicCalendarEvent event, AcademicCalendarEventRequest request) {
        event.setTitle(request.getTitle().trim());
        event.setDescription(trimToNull(request.getDescription()));
        event.setEventType(request.getEventType());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        boolean allDay = Boolean.TRUE.equals(request.getAllDay());
        event.setAllDay(allDay);
        event.setStartTime(allDay ? null : request.getStartTime());
        event.setEndTime(allDay ? null : request.getEndTime());
        event.setLocation(trimToNull(request.getLocation()));
        event.setAudienceType(request.getAudienceType());
    }

    private void replaceAudienceMappings(AcademicCalendarEvent event, AcademicCalendarEventRequest request) {
        initializeMappings(event);
        event.getClasses().clear();
        event.getSections().clear();

        if (request.getAudienceType() == CalendarAudienceType.CLASS && request.getClassIds() != null) {
            for (Long classId : request.getClassIds()) {
                AcademicClass cls = classRepository.findByIdWithYear(classId)
                        .orElseThrow(() -> new BusinessException("Class not found: " + classId));
                AcademicCalendarEventClass link = new AcademicCalendarEventClass();
                link.setEvent(event);
                link.setAcademicClass(cls);
                event.getClasses().add(link);
            }
        }
        if (request.getAudienceType() == CalendarAudienceType.SECTION && request.getSectionIds() != null) {
            for (Long sectionId : request.getSectionIds()) {
                AcademicSection section = sectionRepository.findByIdWithClass(sectionId)
                        .orElseThrow(() -> new BusinessException("Section not found: " + sectionId));
                AcademicCalendarEventSection link = new AcademicCalendarEventSection();
                link.setEvent(event);
                link.setSection(section);
                event.getSections().add(link);
            }
        }
    }

    private void markPublished(AcademicCalendarEvent event) {
        event.setStatus(CalendarEventStatus.PUBLISHED);
        event.setPublishedBy(accessGuard.currentUsernameOrNull());
        event.setPublishedOn(LocalDateTime.now());
    }

    private void initializeMappings(AcademicCalendarEvent event) {
        Hibernate.initialize(event.getClasses());
        Hibernate.initialize(event.getSections());
        for (AcademicCalendarEventClass link : event.getClasses()) {
            Hibernate.initialize(link.getAcademicClass());
        }
        for (AcademicCalendarEventSection link : event.getSections()) {
            Hibernate.initialize(link.getSection());
            if (link.getSection() != null) {
                Hibernate.initialize(link.getSection().getAcademicClass());
            }
        }
    }

    private AcademicCalendarEventResponse toResponse(AcademicCalendarEvent event) {
        initializeMappings(event);
        AcademicYear year = event.getAcademicYear();

        List<AcademicCalendarEventResponse.CalendarClassRef> classes = event.getClasses().stream()
                .map(link -> AcademicCalendarEventResponse.CalendarClassRef.builder()
                        .classId(link.getAcademicClass().getClassId())
                        .name(link.getAcademicClass().getName())
                        .code(link.getAcademicClass().getCode())
                        .build())
                .collect(Collectors.toList());

        List<AcademicCalendarEventResponse.CalendarSectionRef> sections = event.getSections().stream()
                .map(link -> AcademicCalendarEventResponse.CalendarSectionRef.builder()
                        .sectionId(link.getSection().getSectionId())
                        .name(link.getSection().getName())
                        .code(link.getSection().getCode())
                        .className(link.getSection().getAcademicClass() != null
                                ? link.getSection().getAcademicClass().getName() : null)
                        .build())
                .collect(Collectors.toList());

        return AcademicCalendarEventResponse.builder()
                .eventId(event.getEventId())
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .yearReadOnly(isYearReadOnly(year))
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .allDay(event.isAllDay())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .audienceType(event.getAudienceType())
                .status(event.getStatus())
                .classes(classes)
                .sections(sections)
                .publishedBy(event.getPublishedBy())
                .publishedOn(event.getPublishedOn())
                .createdBy(event.getCreatedBy())
                .createdOn(event.getCreatedOn())
                .updatedBy(event.getUpdatedBy())
                .updatedOn(event.getUpdatedOn())
                .build();
    }

    private long countType(List<AcademicCalendarEventResponse> events, CalendarEventType type) {
        return events.stream().filter(e -> e.getEventType() == type).count();
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify calendar for an inactive academic year");
        }
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private AcademicCalendarEvent requireEvent(Long eventId) {
        return eventRepository.findByIdWithYear(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar event not found: " + eventId));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record AudienceScope(boolean viewAll, Set<Long> classIds, Set<Long> sectionIds) {
        static AudienceScope all() {
            return new AudienceScope(true, Set.of(), Set.of());
        }

        static AudienceScope empty() {
            return new AudienceScope(false, Set.of(), Set.of());
        }
    }
}
