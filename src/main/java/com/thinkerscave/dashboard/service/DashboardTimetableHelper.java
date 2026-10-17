package com.thinkerscave.dashboard.service;

import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.attendance.service.TeacherAttendanceAuthorizationService;
import com.thinkerscave.dashboard.dto.response.widgetdata.TimetableSlotItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves a user's real "today's schedule" from the published timetable —
 * shared by the Staff (teacher view) and Student/Parent (section view) dashboards.
 *
 * <p>Queries only today's {@link TimetableEntry} rows directly (never the whole
 * published grid via {@code TimetableService.getGrid}), which avoids an N+1
 * primary-teacher lookup per entry across the entire academic year.
 */
@Component
@RequiredArgsConstructor
public class DashboardTimetableHelper {

    private final AcademicYearRepository academicYearRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TeacherAttendanceAuthorizationService teacherAttendanceAuthorizationService;

    @Transactional(readOnly = true)
    public List<TimetableSlotItem> todaySlotsForTeacher(Long staffId) {
        if (staffId == null) {
            return List.of();
        }
        return teacherAttendanceAuthorizationService.slotsForTeacher(staffId, LocalDate.now()).stream()
                .map(slot -> toSlot(slot.entry(), slot))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimetableSlotItem> todaySlotsForSection(Long sectionId) {
        if (sectionId == null) {
            return List.of();
        }
        Long versionId = currentPublishedVersionId();
        if (versionId == null) {
            return List.of();
        }

        return timetableEntryRepository
                .findByTimetableVersion_TimetableVersionIdAndSection_SectionId(versionId, sectionId).stream()
                .filter(e -> e.getDayOfWeek() == today())
                .sorted(Comparator.comparing(e -> e.getTimetablePeriod().getPeriodNumber()))
                .map(entry -> toSlot(entry, null))
                .collect(Collectors.toList());
    }

    private com.thinkerscave.academics.enums.DayOfWeek today() {
        return com.thinkerscave.academics.enums.DayOfWeek.valueOf(LocalDate.now().getDayOfWeek().name());
    }

    private Long currentPublishedVersionId() {
        return academicYearRepository.findByCurrentYearTrue()
                .flatMap(year -> timetableVersionRepository
                        .findByAcademicYear_AcademicYearIdAndStatus(year.getAcademicYearId(), TimetableStatus.PUBLISHED))
                .map(TimetableVersion::getTimetableVersionId)
                .orElse(null);
    }

        private TimetableSlotItem toSlot(
            TimetableEntry entry, TeacherAttendanceAuthorizationService.TeacherTeachingSlot assignment) {
        var period = entry.getTimetablePeriod();
        var section = entry.getSection();
        var academicClass = section != null ? section.getAcademicClass() : null;
        var academicYear = entry.getTimetableVersion() != null
            ? entry.getTimetableVersion().getAcademicYear() : null;
        return TimetableSlotItem.builder()
            .timetableEntryId(entry.getTimetableEntryId())
            .academicYearId(academicYear != null ? academicYear.getAcademicYearId() : null)
            .classId(academicClass != null ? academicClass.getClassId() : null)
            .sectionId(section != null ? section.getSectionId() : null)
            .periodId(period != null ? period.getTimetablePeriodId() : null)
                .periodNumber(period != null && period.getPeriodNumber() != null ? period.getPeriodNumber().intValue() : null)
                .periodName(period != null ? period.getName() : null)
                .startTime(period != null ? period.getStartTime() : null)
                .endTime(period != null ? period.getEndTime() : null)
                .subjectName(entry.getSubjectNameSnapshot())
                .className(section != null && section.getAcademicClass() != null
                        ? section.getAcademicClass().getName() + " " + section.getName() : null)
                .roomLabel(entry.getResource() != null ? entry.getResource().getName() : null)
                .attendanceMode(assignment != null ? assignment.attendanceMode().name() : null)
                .attendanceAllowed(assignment != null && assignment.attendanceAllowed())
                .substitute(assignment != null && assignment.substitute())
                .build();
    }
}

