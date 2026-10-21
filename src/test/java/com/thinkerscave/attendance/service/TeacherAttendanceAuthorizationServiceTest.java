package com.thinkerscave.attendance.service;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.entity.TeacherArrangement;
import com.thinkerscave.academics.entity.TeacherArrangementSlot;
import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TeacherArrangementStatus;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TeacherArrangementRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.attendance.enums.AttendanceMode;
import com.thinkerscave.attendance.security.AttendanceAccessGuard;
import com.thinkerscave.settings.ModuleSettingCatalog;
import com.thinkerscave.settings.ModuleSettingsService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.repository.StaffRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAttendanceAuthorizationServiceTest {

    private static final Long ORG_ID = 268L;
    private static final Long YEAR_ID = 7L;
    private static final Long CLASS_ID = 10L;
    private static final Long SECTION_ID = 11L;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 19);

    @Mock private AttendanceAccessGuard accessGuard;
    @Mock private StaffRepository staffRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private TimetableVersionRepository timetableVersionRepository;
    @Mock private TimetableEntryRepository timetableEntryRepository;
    @Mock private TeacherAllocationTeacherRepository allocationTeacherRepository;
    @Mock private TeacherArrangementRepository arrangementRepository;
    @Mock private ModuleSettingsService moduleSettingsService;

    private TeacherAttendanceAuthorizationService service;

    @BeforeEach
    void setUp() {
        OrganizationContext.setOrganizationId(ORG_ID);
        service = new TeacherAttendanceAuthorizationService(
                accessGuard, staffRepository, academicYearRepository, timetableVersionRepository,
                timetableEntryRepository, allocationTeacherRepository, arrangementRepository,
                moduleSettingsService);
    }

    @AfterEach
    void clearContext() {
        OrganizationContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void dailyAttendanceIsLimitedToTheFirstTeacherForEachSection() {
        Schedule schedule = schedule(AttendanceMode.DAILY, List.of());

        List<TeacherAttendanceAuthorizationService.TeacherTeachingSlot> firstTeacherSlots =
                service.slotsForTeacher(1L, DATE);
        List<TeacherAttendanceAuthorizationService.TeacherTeachingSlot> laterTeacherSlots =
                service.slotsForTeacher(2L, DATE);

        assertEquals(1, firstTeacherSlots.size());
        assertTrue(firstTeacherSlots.get(0).attendanceAllowed());
        assertEquals(1, laterTeacherSlots.size());
        assertFalse(laterTeacherSlots.get(0).attendanceAllowed());

        authenticateAsStaff(102L);
        when(accessGuard.hasElevatedRole()).thenReturn(false);
        when(accessGuard.requireCurrentUser()).thenReturn(user(102L));
        when(staffRepository.findByUser_Id(102L)).thenReturn(Optional.of(schedule.laterTeacher));

        assertThrows(AccessDeniedException.class,
                () -> service.requireDailyAttendanceAccess(CLASS_ID, SECTION_ID, DATE));
    }

    @Test
    void periodWiseAttendanceAllowsTheTeacherAssignedToThatPeriod() {
        Schedule schedule = schedule(AttendanceMode.PERIOD_WISE, List.of());
        authenticateAsStaff(102L);
        when(accessGuard.hasElevatedRole()).thenReturn(false);
        when(accessGuard.requireCurrentUser()).thenReturn(user(102L));
        when(staffRepository.findByUser_Id(102L)).thenReturn(Optional.of(schedule.laterTeacher));

        TimetableEntry authorized = service.requirePeriodAttendanceAccess(CLASS_ID, SECTION_ID, DATE, 32L);

        assertEquals(102L, authorized.getTeacherAllocation().getTeacherAllocationId());
    }

    @Test
    void periodWiseSectionLoadDistinguishesUnassignedTeacherFromMissingPeriod() {
        schedule(AttendanceMode.PERIOD_WISE, List.of());
        when(accessGuard.hasElevatedRole()).thenReturn(false);

        User unassignedUser = user(104L);
        authenticateAsStaff(104L);
        when(accessGuard.requireCurrentUser()).thenReturn(unassignedUser);
        when(staffRepository.findByUser_Id(104L)).thenReturn(Optional.of(staff(4L, 104L)));
        AccessDeniedException denied = assertThrows(AccessDeniedException.class,
                () -> service.requirePeriodSectionAccess(CLASS_ID, SECTION_ID, DATE));
        assertEquals("You are not assigned to teach this section today.", denied.getMessage());

        User assignedUser = user(101L);
        authenticateAsStaff(101L);
        when(accessGuard.requireCurrentUser()).thenReturn(assignedUser);
        when(staffRepository.findByUser_Id(101L)).thenReturn(Optional.of(staff(1L, 101L)));
        BadRequestException choosePeriod = assertThrows(BadRequestException.class,
                () -> service.requirePeriodSectionAccess(CLASS_ID, SECTION_ID, DATE));
        assertEquals("Choose one of your scheduled periods before loading attendance.", choosePeriod.getMessage());
    }

    @Test
    void approvedSubstituteTakesOverTheScheduledSlotAndDailyAttendanceEligibility() {
        TeacherArrangement arrangement = new TeacherArrangement();
        arrangement.setStatus(TeacherArrangementStatus.APPROVED);
        TimetableEntry firstPeriod = entry(21L, 31L, (short) 1, allocation(101L));
        TeacherArrangementSlot slot = new TeacherArrangementSlot();
        slot.setTimetableEntry(firstPeriod);
        slot.setSubstituteTeacher(staff(3L, 103L));
        arrangement.setSlots(List.of(slot));
        schedule(AttendanceMode.DAILY, List.of(arrangement));

        List<TeacherAttendanceAuthorizationService.TeacherTeachingSlot> substituteSlots =
                service.slotsForTeacher(3L, DATE);
        List<TeacherAttendanceAuthorizationService.TeacherTeachingSlot> absentTeacherSlots =
                service.slotsForTeacher(1L, DATE);

        assertEquals(1, substituteSlots.size());
        assertTrue(substituteSlots.get(0).substitute());
        assertTrue(substituteSlots.get(0).attendanceAllowed());
        assertTrue(absentTeacherSlots.isEmpty());
    }

    private Schedule schedule(AttendanceMode mode, List<TeacherArrangement> arrangements) {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(YEAR_ID);
        TimetableVersion version = new TimetableVersion();
        version.setTimetableVersionId(19L);
        version.setAcademicYear(year);

        Staff firstTeacher = staff(1L, 101L);
        Staff laterTeacher = staff(2L, 102L);
        TeacherAllocation firstAllocation = allocation(101L);
        TeacherAllocation laterAllocation = allocation(102L);
        TimetableEntry firstEntry = entry(21L, 31L, (short) 1, firstAllocation);
        TimetableEntry laterEntry = entry(22L, 32L, (short) 2, laterAllocation);
        firstEntry.setTimetableVersion(version);
        laterEntry.setTimetableVersion(version);

        when(academicYearRepository.findByCurrentYearTrue()).thenReturn(Optional.of(year));
        when(timetableVersionRepository.findByAcademicYear_AcademicYearIdAndStatus(
                YEAR_ID, TimetableStatus.PUBLISHED)).thenReturn(Optional.of(version));
        when(timetableEntryRepository.findByTimetableVersion_TimetableVersionIdAndDayOfWeek(
                19L, DayOfWeek.MONDAY)).thenReturn(List.of(firstEntry, laterEntry));
        when(allocationTeacherRepository.findActiveByYear(YEAR_ID)).thenReturn(List.of(
                primary(firstAllocation, firstTeacher), primary(laterAllocation, laterTeacher)));
        when(arrangementRepository.findByAcademicYear_AcademicYearIdAndAbsenceDateAndStatusOrderByTeacherArrangementIdAsc(
                YEAR_ID, DATE, TeacherArrangementStatus.APPROVED)).thenReturn(arrangements);
        when(moduleSettingsService.getString(ORG_ID, ModuleSettingCatalog.ATTENDANCE,
                ModuleSettingCatalog.ATTENDANCE_MODE)).thenReturn(mode.name());
        return new Schedule(firstTeacher, laterTeacher);
    }

    private void authenticateAsStaff(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "teacher", "secret", List.of(new SimpleGrantedAuthority("STAFF"))));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Staff staff(Long staffId, Long userId) {
        Staff staff = new Staff();
        staff.setStaffId(staffId);
        staff.setUser(user(userId));
        staff.setActive(true);
        staff.setEmploymentStatus(EmploymentStatus.ACTIVE);
        return staff;
    }

    private TeacherAllocation allocation(Long id) {
        TeacherAllocation allocation = new TeacherAllocation();
        allocation.setTeacherAllocationId(id);
        allocation.setActive(true);
        return allocation;
    }

    private TeacherAllocationTeacher primary(TeacherAllocation allocation, Staff staff) {
        TeacherAllocationTeacher assignment = new TeacherAllocationTeacher();
        assignment.setTeacherAllocation(allocation);
        assignment.setStaff(staff);
        assignment.setRole(TeacherAllocationTeacherRole.PRIMARY);
        assignment.setActive(true);
        assignment.setEffectiveFrom(DATE.minusDays(30));
        return assignment;
    }

    private TimetableEntry entry(Long entryId, Long periodId, short periodNumber, TeacherAllocation allocation) {
        AcademicClass academicClass = new AcademicClass();
        academicClass.setClassId(CLASS_ID);
        academicClass.setName("Class VIII");
        AcademicSection section = new AcademicSection();
        section.setSectionId(SECTION_ID);
        section.setName("A");
        section.setAcademicClass(academicClass);
        TimetablePeriod period = new TimetablePeriod();
        period.setTimetablePeriodId(periodId);
        period.setPeriodNumber(periodNumber);
        period.setName("Period " + periodNumber);
        period.setStartTime(LocalTime.of(8, 0).plusHours(periodNumber - 1));
        period.setEndTime(LocalTime.of(8, 45).plusHours(periodNumber - 1));
        TimetableEntry entry = new TimetableEntry();
        entry.setTimetableEntryId(entryId);
        entry.setDayOfWeek(DayOfWeek.MONDAY);
        entry.setEntryType(TimetableEntryType.SUBJECT);
        entry.setSection(section);
        entry.setTimetablePeriod(period);
        entry.setTeacherAllocation(allocation);
        entry.setSubjectNameSnapshot("English");
        return entry;
    }

    private record Schedule(Staff firstTeacher, Staff laterTeacher) {}
}