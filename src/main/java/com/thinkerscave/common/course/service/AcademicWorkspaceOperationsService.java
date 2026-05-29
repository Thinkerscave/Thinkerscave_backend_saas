package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.domain.AcademicCalendarEvent;
import com.thinkerscave.common.course.domain.AcademicSetting;
import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.ClassTeacherAssignment;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.domain.TimetableSlot;
import com.thinkerscave.common.course.dto.AcademicCalendarEventDTO;
import com.thinkerscave.common.course.dto.AcademicSettingDTO;
import com.thinkerscave.common.course.dto.ClassTeacherAssignmentDTO;
import com.thinkerscave.common.course.dto.TimetableSlotDTO;
import com.thinkerscave.common.course.enums.AcademicEventType;
import com.thinkerscave.common.course.enums.AcademicSettingValueType;
import com.thinkerscave.common.course.repository.AcademicCalendarEventRepository;
import com.thinkerscave.common.course.repository.AcademicSettingRepository;
import com.thinkerscave.common.course.repository.AcademicYearRepository;
import com.thinkerscave.common.course.repository.ClassTeacherAssignmentRepository;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.course.repository.TimetableSlotRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.staff.repository.StaffRepository;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicWorkspaceOperationsService {

    private final OrganizationRepository organizationRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StaffRepository staffRepository;
    private final SubjectRepository subjectRepository;
    private final ClassTeacherAssignmentRepository classTeacherAssignmentRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final AcademicSettingRepository academicSettingRepository;

    public List<ClassTeacherAssignmentDTO> listClassTeacherAssignments(Long organizationId, Long academicYearId) {
        Organisation organization = resolveOrganization(organizationId);
        AcademicYear academicYear = resolveAcademicYear(academicYearId, organization);
        return classTeacherAssignmentRepository
                .findByOrganizationAndAcademicYearAndIsActiveTrueOrderByClassEntity_ClassNameAscSection_SectionNameAsc(
                        organization, academicYear)
                .stream()
                .map(this::toClassTeacherDto)
                .toList();
    }

    @Transactional
    public ClassTeacherAssignmentDTO saveClassTeacherAssignment(ClassTeacherAssignmentDTO dto) {
        Organisation organization = resolveOrganization(dto.getOrganizationId());
        AcademicYear academicYear = resolveAcademicYear(dto.getAcademicYearId(), organization);
        ClassEntity classEntity = resolveClass(dto.getClassId(), organization);
        Section section = resolveSection(dto.getSectionId(), classEntity);
        Staff teacher = resolveStaff(dto.getTeacherId(), organization);

        Long excludeId = dto.getAssignmentId();
        if (classTeacherAssignmentRepository.existsActiveForClassSection(organization, academicYear, classEntity, section, excludeId)) {
            throw new BadRequestException("A class teacher is already assigned for this class and section.");
        }

        if (classTeacherAssignmentRepository.existsActiveForTeacher(organization, academicYear, teacher, excludeId)) {
            throw new BadRequestException("This teacher already owns another class in the selected academic year.");
        }

        ClassTeacherAssignment assignment = excludeId == null
                ? new ClassTeacherAssignment()
                : classTeacherAssignmentRepository.findByAssignmentIdAndOrganization(excludeId, organization)
                        .orElseThrow(() -> new ResourceNotFoundException("Class teacher assignment not found"));

        assignment.setOrganization(organization);
        assignment.setAcademicYear(academicYear);
        assignment.setClassEntity(classEntity);
        assignment.setSection(section);
        assignment.setClassTeacher(teacher);
        assignment.setEffectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : academicYear.getStartDate());
        assignment.setEffectiveTo(dto.getEffectiveTo());
        assignment.setIsActive(dto.getIsActive() == null || dto.getIsActive());
        assignment.setNotes(trimToNull(dto.getNotes()));

        return toClassTeacherDto(classTeacherAssignmentRepository.save(assignment));
    }

    @Transactional
    public void deactivateClassTeacherAssignment(Long organizationId, Long assignmentId) {
        Organisation organization = resolveOrganization(organizationId);
        ClassTeacherAssignment assignment = classTeacherAssignmentRepository.findByAssignmentIdAndOrganization(assignmentId, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Class teacher assignment not found"));
        assignment.setIsActive(false);
        classTeacherAssignmentRepository.save(assignment);
    }

    public List<TimetableSlotDTO> listTimetableSlots(Long organizationId, Long academicYearId, Long classId, Long teacherId) {
        Organisation organization = resolveOrganization(organizationId);
        AcademicYear academicYear = resolveAcademicYear(academicYearId, organization);
        return timetableSlotRepository
                .findByOrganizationAndAcademicYearAndIsActiveTrueOrderByDayOfWeekAscPeriodNumberAsc(organization, academicYear)
                .stream()
                .filter(slot -> classId == null || slot.getClassEntity().getClassId().equals(classId))
                .filter(slot -> teacherId == null || slot.getTeacher().getId().equals(teacherId))
                .map(this::toTimetableSlotDto)
                .toList();
    }

    @Transactional
    public TimetableSlotDTO saveTimetableSlot(TimetableSlotDTO dto) {
        Organisation organization = resolveOrganization(dto.getOrganizationId());
        AcademicYear academicYear = resolveAcademicYear(dto.getAcademicYearId(), organization);
        ClassEntity classEntity = resolveClass(dto.getClassId(), organization);
        Section section = resolveSection(dto.getSectionId(), classEntity);
        Subject subject = resolveSubject(dto.getSubjectId(), organization);
        Staff teacher = resolveStaff(dto.getTeacherId(), organization);
        DayOfWeek dayOfWeek = requireValue(dto.getDayOfWeek(), "Day of week is required");
        Integer periodNumber = requirePositive(dto.getPeriodNumber(), "Period number is required");

        if (dto.getStartTime() != null && dto.getEndTime() != null && !dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BadRequestException("Timetable slot end time must be after start time.");
        }

        Long excludeId = dto.getSlotId();
        if (timetableSlotRepository.existsClassSlotConflict(organization, academicYear, classEntity, section, dayOfWeek, periodNumber, excludeId)) {
            throw new BadRequestException("This class already has a timetable slot at the selected period.");
        }

        if (timetableSlotRepository.existsTeacherSlotConflict(organization, academicYear, teacher, dayOfWeek, periodNumber, excludeId)) {
            throw new BadRequestException("This teacher already has a timetable slot at the selected period.");
        }

        TimetableSlot slot = excludeId == null
                ? new TimetableSlot()
                : timetableSlotRepository.findBySlotIdAndOrganization(excludeId, organization)
                        .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));

        slot.setOrganization(organization);
        slot.setAcademicYear(academicYear);
        slot.setClassEntity(classEntity);
        slot.setSection(section);
        slot.setSubject(subject);
        slot.setTeacher(teacher);
        slot.setDayOfWeek(dayOfWeek);
        slot.setPeriodNumber(periodNumber);
        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());
        slot.setRoomName(trimToNull(dto.getRoomName()));
        slot.setIsActive(dto.getIsActive() == null || dto.getIsActive());

        return toTimetableSlotDto(timetableSlotRepository.save(slot));
    }

    @Transactional
    public void deactivateTimetableSlot(Long organizationId, Long slotId) {
        Organisation organization = resolveOrganization(organizationId);
        TimetableSlot slot = timetableSlotRepository.findBySlotIdAndOrganization(slotId, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));
        slot.setIsActive(false);
        timetableSlotRepository.save(slot);
    }

    public List<AcademicCalendarEventDTO> listCalendarEvents(Long organizationId, Long academicYearId) {
        Organisation organization = resolveOrganization(organizationId);
        AcademicYear academicYear = resolveAcademicYear(academicYearId, organization);
        return academicCalendarEventRepository
                .findByOrganizationAndAcademicYearAndIsActiveTrueOrderByStartDateAscEndDateAsc(organization, academicYear)
                .stream()
                .map(this::toCalendarEventDto)
                .toList();
    }

    @Transactional
    public AcademicCalendarEventDTO saveCalendarEvent(AcademicCalendarEventDTO dto) {
        Organisation organization = resolveOrganization(dto.getOrganizationId());
        AcademicYear academicYear = resolveAcademicYear(dto.getAcademicYearId(), organization);
        LocalDate startDate = requireValue(dto.getStartDate(), "Event start date is required");
        LocalDate endDate = dto.getEndDate() == null ? startDate : dto.getEndDate();
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Event end date cannot be before start date.");
        }

        String title = trimToNull(dto.getTitle());
        if (title == null) {
            throw new BadRequestException("Event title is required.");
        }

        AcademicCalendarEvent event = dto.getEventId() == null
                ? new AcademicCalendarEvent()
                : academicCalendarEventRepository.findByEventIdAndOrganization(dto.getEventId(), organization)
                        .orElseThrow(() -> new ResourceNotFoundException("Academic calendar event not found"));

        event.setOrganization(organization);
        event.setAcademicYear(academicYear);
        event.setTitle(title);
        event.setEventType(dto.getEventType() == null ? AcademicEventType.EVENT : dto.getEventType());
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setAllDay(dto.getAllDay() == null || dto.getAllDay());
        event.setIsActive(dto.getIsActive() == null || dto.getIsActive());
        event.setDescription(trimToNull(dto.getDescription()));

        return toCalendarEventDto(academicCalendarEventRepository.save(event));
    }

    @Transactional
    public void deactivateCalendarEvent(Long organizationId, Long eventId) {
        Organisation organization = resolveOrganization(organizationId);
        AcademicCalendarEvent event = academicCalendarEventRepository.findByEventIdAndOrganization(eventId, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Academic calendar event not found"));
        event.setIsActive(false);
        academicCalendarEventRepository.save(event);
    }

    public List<AcademicSettingDTO> listAcademicSettings(Long organizationId) {
        Organisation organization = resolveOrganization(organizationId);
        List<AcademicSettingDTO> settings = academicSettingRepository
                .findByOrganizationAndIsActiveTrueOrderByCategoryAscSettingKeyAsc(organization)
                .stream()
                .map(this::toSettingDto)
                .toList();

        return settings.isEmpty() ? defaultSettings(organization) : settings;
    }

    @Transactional
    public AcademicSettingDTO saveAcademicSetting(AcademicSettingDTO dto) {
        Organisation organization = resolveOrganization(dto.getOrganizationId());
        String settingKey = trimToNull(dto.getSettingKey());
        if (settingKey == null) {
            throw new BadRequestException("Setting key is required.");
        }

        AcademicSetting setting = dto.getSettingId() == null
                ? academicSettingRepository.findByOrganizationAndSettingKey(organization, settingKey)
                        .orElseGet(AcademicSetting::new)
                : academicSettingRepository.findBySettingIdAndOrganization(dto.getSettingId(), organization)
                        .orElseThrow(() -> new ResourceNotFoundException("Academic setting not found"));

        setting.setOrganization(organization);
        setting.setSettingKey(settingKey);
        setting.setSettingValue(dto.getSettingValue());
        setting.setValueType(dto.getValueType() == null ? AcademicSettingValueType.TEXT : dto.getValueType());
        setting.setCategory(trimToNull(dto.getCategory()) == null ? "GENERAL" : trimToNull(dto.getCategory()).toUpperCase());
        setting.setIsActive(dto.getIsActive() == null || dto.getIsActive());
        setting.setDescription(trimToNull(dto.getDescription()));

        return toSettingDto(academicSettingRepository.save(setting));
    }

    private Organisation resolveOrganization(Long organizationId) {
        Long id = requirePositive(organizationId, "Organization is required");
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    private AcademicYear resolveAcademicYear(Long academicYearId, Organisation organization) {
        Long id = requirePositive(academicYearId, "Academic year is required");
        AcademicYear academicYear = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        if (academicYear.getOrganization() == null || !organization.getOrgId().equals(academicYear.getOrganization().getOrgId())) {
            throw new BadRequestException("Academic year does not belong to the selected organization.");
        }
        return academicYear;
    }

    private ClassEntity resolveClass(Long classId, Organisation organization) {
        Long id = requirePositive(classId, "Class is required");
        ClassEntity classEntity = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        if (classEntity.getOrganizationId() != null && !organization.getOrgId().equals(classEntity.getOrganizationId())) {
            throw new BadRequestException("Class does not belong to the selected organization.");
        }
        return classEntity;
    }

    private Section resolveSection(Long sectionId, ClassEntity classEntity) {
        if (sectionId == null) {
            return null;
        }

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        Long sectionClassId = section.getClassEntity() == null ? null : section.getClassEntity().getClassId();
        if (sectionClassId != null && !sectionClassId.equals(classEntity.getClassId())) {
            throw new BadRequestException("Section does not belong to the selected class.");
        }
        return section;
    }

    private Staff resolveStaff(Long staffId, Organisation organization) {
        Long id = requirePositive(staffId, "Teacher is required");
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        if (staff.getOrganizationId() != null && !organization.getOrgId().equals(staff.getOrganizationId())) {
            throw new BadRequestException("Teacher does not belong to the selected organization.");
        }
        if (Boolean.FALSE.equals(staff.getIsActive())) {
            throw new BadRequestException("Teacher is inactive.");
        }
        return staff;
    }

    private Subject resolveSubject(Long subjectId, Organisation organization) {
        Long id = requirePositive(subjectId, "Subject is required");
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (subject.getOrganization() != null && !organization.getOrgId().equals(subject.getOrganization().getOrgId())) {
            throw new BadRequestException("Subject does not belong to the selected organization.");
        }
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new BadRequestException("Subject is inactive.");
        }
        return subject;
    }

    private <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private Long requirePositive(Long value, String message) {
        if (value == null || value <= 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private Integer requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String staffName(Staff staff) {
        String name = String.join(" ",
                staff.getFirstName() == null ? "" : staff.getFirstName(),
                staff.getMiddleName() == null ? "" : staff.getMiddleName(),
                staff.getLastName() == null ? "" : staff.getLastName()).trim().replaceAll("\\s+", " ");
        return name.isBlank() ? staff.getStaffCode() : name;
    }

    private ClassTeacherAssignmentDTO toClassTeacherDto(ClassTeacherAssignment assignment) {
        return ClassTeacherAssignmentDTO.builder()
                .assignmentId(assignment.getAssignmentId())
                .organizationId(assignment.getOrganization().getOrgId())
                .academicYearId(assignment.getAcademicYear().getAcademicYearId())
                .classId(assignment.getClassEntity().getClassId())
                .className(assignment.getClassEntity().getClassName())
                .sectionId(assignment.getSection() == null ? null : assignment.getSection().getSectionId())
                .sectionName(assignment.getSection() == null ? null : assignment.getSection().getSectionName())
                .teacherId(assignment.getClassTeacher().getId())
                .teacherName(staffName(assignment.getClassTeacher()))
                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())
                .isActive(assignment.getIsActive())
                .notes(assignment.getNotes())
                .build();
    }

    private TimetableSlotDTO toTimetableSlotDto(TimetableSlot slot) {
        return TimetableSlotDTO.builder()
                .slotId(slot.getSlotId())
                .organizationId(slot.getOrganization().getOrgId())
                .academicYearId(slot.getAcademicYear().getAcademicYearId())
                .classId(slot.getClassEntity().getClassId())
                .className(slot.getClassEntity().getClassName())
                .sectionId(slot.getSection() == null ? null : slot.getSection().getSectionId())
                .sectionName(slot.getSection() == null ? null : slot.getSection().getSectionName())
                .subjectId(slot.getSubject().getSubjectId())
                .subjectName(slot.getSubject().getSubjectName())
                .teacherId(slot.getTeacher().getId())
                .teacherName(staffName(slot.getTeacher()))
                .dayOfWeek(slot.getDayOfWeek())
                .periodNumber(slot.getPeriodNumber())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .roomName(slot.getRoomName())
                .isActive(slot.getIsActive())
                .build();
    }

    private AcademicCalendarEventDTO toCalendarEventDto(AcademicCalendarEvent event) {
        return AcademicCalendarEventDTO.builder()
                .eventId(event.getEventId())
                .organizationId(event.getOrganization().getOrgId())
                .academicYearId(event.getAcademicYear().getAcademicYearId())
                .title(event.getTitle())
                .eventType(event.getEventType())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .allDay(event.getAllDay())
                .isActive(event.getIsActive())
                .description(event.getDescription())
                .build();
    }

    private AcademicSettingDTO toSettingDto(AcademicSetting setting) {
        return AcademicSettingDTO.builder()
                .settingId(setting.getSettingId())
                .organizationId(setting.getOrganization().getOrgId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .valueType(setting.getValueType())
                .category(setting.getCategory())
                .isActive(setting.getIsActive())
                .description(setting.getDescription())
                .build();
    }

    private List<AcademicSettingDTO> defaultSettings(Organisation organization) {
        return List.of(
                defaultSetting(organization, "GRADING_SCALE", "LETTER", "ASSESSMENT", "Default grading scale used across academic evaluations."),
                defaultSetting(organization, "PERIODS_PER_DAY", "8", "TIMETABLE", "Default number of periods in a teaching day."),
                defaultSetting(organization, "ATTENDANCE_LINKAGE", "true", "ATTENDANCE", "Controls whether class timetable periods feed attendance workflows."),
                defaultSetting(organization, "PROMOTION_RULE", "ANNUAL_REVIEW", "PROMOTION", "Default promotion evaluation strategy."),
                defaultSetting(organization, "SECTION_CAPACITY", "40", "STRUCTURE", "Default section capacity for new academic sections."));
    }

    private AcademicSettingDTO defaultSetting(Organisation organization, String key, String value, String category, String description) {
        AcademicSettingValueType valueType = value.matches("\\d+") ? AcademicSettingValueType.NUMBER
                : ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) ? AcademicSettingValueType.BOOLEAN : AcademicSettingValueType.TEXT);
        return AcademicSettingDTO.builder()
                .organizationId(organization.getOrgId())
                .settingKey(key)
                .settingValue(value)
                .valueType(valueType)
                .category(category)
                .isActive(true)
                .description(description)
                .build();
    }
}