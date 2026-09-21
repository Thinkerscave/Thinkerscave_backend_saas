package com.thinkerscave.attendance.service;

import com.thinkerscave.academics.entity.AcademicCalendarEvent;
import com.thinkerscave.academics.entity.TimetableConfiguration;
import com.thinkerscave.academics.entity.TimetableWorkingDay;
import com.thinkerscave.academics.enums.CalendarEventStatus;
import com.thinkerscave.academics.enums.CalendarEventType;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableConfigurationStatus;
import com.thinkerscave.academics.repository.AcademicCalendarEventRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableConfigurationRepository;
import com.thinkerscave.academics.repository.TimetableWorkingDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Resolves whether attendance is required for a given org/staff/date using
 * Leave (port) → Holiday (academic calendar) → Weekend (timetable working days).
 */
@Service
@RequiredArgsConstructor
public class StaffAttendanceDayContextService {

    private final StaffLeaveAttendancePort leaveAttendancePort;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicCalendarEventRepository academicCalendarEventRepository;
    private final TimetableConfigurationRepository timetableConfigurationRepository;
    private final TimetableWorkingDayRepository timetableWorkingDayRepository;

    public DayContext resolve(Long organizationId, Long staffId, LocalDate date) {
        Optional<StaffLeaveAttendancePort.ApprovedLeaveInfo> leave =
                leaveAttendancePort.findApprovedLeave(organizationId, staffId, date);
        if (leave.isPresent()) {
            StaffLeaveAttendancePort.ApprovedLeaveInfo info = leave.get();
            String label = info.leaveLabel() != null && !info.leaveLabel().isBlank()
                    ? info.leaveLabel()
                    : "Approved Leave";
            return DayContext.onLeave(label);
        }

        Optional<AcademicCalendarEvent> holiday = findHoliday(date);
        if (holiday.isPresent()) {
            return DayContext.forHoliday(holiday.get().getTitle());
        }

        if (isWeekend(date)) {
            return DayContext.forWeekend();
        }

        return DayContext.workingDay();
    }

    private Optional<AcademicCalendarEvent> findHoliday(LocalDate date) {
        return academicYearRepository.findByCurrentYearTrue()
                .map(year -> academicCalendarEventRepository.findWithYearByAcademicYearId(year.getAcademicYearId()))
                .orElse(List.of())
                .stream()
                .filter(e -> e.getEventType() == CalendarEventType.HOLIDAY)
                .filter(e -> e.getStatus() == CalendarEventStatus.PUBLISHED)
                .filter(e -> !date.isBefore(e.getStartDate()) && !date.isAfter(e.getEndDate()))
                .findFirst();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = DayOfWeek.valueOf(date.getDayOfWeek().name());

        Optional<Long> yearId = academicYearRepository.findByCurrentYearTrue()
                .map(y -> y.getAcademicYearId());
        if (yearId.isEmpty()) {
            return isDefaultWeekend(date);
        }

        List<TimetableConfiguration> configs = timetableConfigurationRepository
                .findByAcademicYear_AcademicYearIdAndStatus(yearId.get(), TimetableConfigurationStatus.READY);
        if (configs.isEmpty()) {
            configs = timetableConfigurationRepository.findByAcademicYear_AcademicYearId(yearId.get());
        }
        if (configs.isEmpty()) {
            return isDefaultWeekend(date);
        }

        // Prefer the first READY (or any) configuration's working-day map.
        TimetableConfiguration config = configs.get(0);
        Optional<TimetableWorkingDay> workingDay = timetableWorkingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationIdAndDayOfWeek(
                        config.getTimetableConfigurationId(), day);

        if (workingDay.isPresent()) {
            return !Boolean.TRUE.equals(workingDay.get().getWorking());
        }

        // Config exists but day not listed → treat as non-working.
        List<TimetableWorkingDay> allDays = timetableWorkingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(config.getTimetableConfigurationId());
        if (!allDays.isEmpty()) {
            return true;
        }

        return isDefaultWeekend(date);
    }

    private boolean isDefaultWeekend(LocalDate date) {
        java.time.DayOfWeek dow = date.getDayOfWeek();
        return dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY;
    }

    public record DayContext(
            boolean attendanceRequired,
            boolean onLeave,
            boolean holiday,
            boolean weekend,
            String holidayName,
            String leaveLabel,
            String reason
    ) {
        public static DayContext workingDay() {
            return new DayContext(true, false, false, false, null, null, null);
        }

        public static DayContext onLeave(String leaveLabel) {
            return new DayContext(false, true, false, false, null, leaveLabel, "APPROVED_LEAVE");
        }

        public static DayContext forHoliday(String holidayName) {
            return new DayContext(false, false, true, false, holidayName, null, "HOLIDAY");
        }

        public static DayContext forWeekend() {
            return new DayContext(false, false, false, true, null, null, "WEEKEND");
        }
    }
}
