package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Row;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.FixedActivity;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.enums.TeacherWorkloadStatus;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableConflictRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Lesson;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacher;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacherReason;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Unit;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningDataLoader;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherWorkloadServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private TimetablePlanningDataLoader planningDataLoader;
    @Mock private TimetableEntryRepository timetableEntryRepository;
    @Mock private TimetableConflictRepository timetableConflictRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    private TeacherWorkloadServiceImpl service;
    private AcademicYear year;
    private Staff staff1;
    private Staff staff2;
    private Staff staff3;

    @BeforeEach
    void setUp() {
        service = new TeacherWorkloadServiceImpl(
                academicYearRepository,
                staffRepository,
                planningDataLoader,
                timetableEntryRepository,
                timetableConflictRepository,
                accessGuard);

        year = new AcademicYear();
        year.setAcademicYearId(7L);
        year.setName("2026-27");
        year.setStatus(AcademicYearStatus.CURRENT);

        staff1 = staff(1L, "Asha", "Rao", "EMP-001");
        staff2 = staff(2L, "Bilal", "Khan", "EMP-002");
        staff3 = staff(3L, "Chen", "Wei", "EMP-003");

        when(academicYearRepository.findById(7L)).thenReturn(Optional.of(year));
        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(staff1, staff2, staff3));
    }

    @Test
    void usesHighestCapacityAcrossDifferentGroupPatterns() {
        Unit sectionA = unit(101L, group(11L, "Grade 6"), section(101L, "A"));
        Unit sectionB = unit(102L, group(12L, "Grade 7"), section(102L, "B"));
        Map<DayOfWeek, List<TimetablePeriod>> fullWeek = week(6, 8);
        Map<DayOfWeek, List<TimetablePeriod>> shorterWeek = week(5, 8);

        TimetablePlanningData data = data(
                List.of(sectionA, sectionB),
                Map.of(101L, fullWeek, 102L, shorterWeek),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        TeacherWorkloadDashboardResponse response = service.getDashboard(7L);

        assertTrue(response.isTargetAvailable());
        assertEquals(48, response.getBenchmarkWeeklyCapacity());
        assertEquals(38, response.getExpectedTarget());
        assertEquals(48, response.getMaximumWeeklyCapacity());
        assertEquals(43, response.getNearMaximumThreshold());
        assertEquals(20, response.getBufferPercentage());
        assertEquals("ALLOCATION_ESTIMATE", response.getWorkloadBasis());
    }

    @Test
    void reportsTargetUnavailableWhenNoPatternHasTeachingSlots() {
        Unit sectionA = unit(101L, group(11L, "Grade 6"), section(101L, "A"));
        Map<DayOfWeek, List<TimetablePeriod>> breaksOnly = new LinkedHashMap<>();
        breaksOnly.put(DayOfWeek.MONDAY, List.of(period(1, TimetableSlotKind.BREAK)));

        TimetablePlanningData data = data(
                List.of(sectionA),
                Map.of(101L, breaksOnly),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        TeacherWorkloadDashboardResponse response = service.getDashboard(7L);

        assertFalse(response.isTargetAvailable());
        assertNull(response.getBenchmarkWeeklyCapacity());
        assertNull(response.getExpectedTarget());
        assertNull(response.getMaximumWeeklyCapacity());
        assertTrue(response.getTargetExplanation().contains("Workload target unavailable"));
        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> "TARGET_UNAVAILABLE".equals(w.getCode())));
        assertTrue(response.getRows().stream()
                .allMatch(r -> r.getStatus() == TeacherWorkloadStatus.TARGET_UNAVAILABLE));
    }

    @Test
    void usesValidCapacityWhenAnotherUnitHasNoSchedulePattern() {
        Unit sectionA = unit(101L, group(11L, "Grade 6"), section(101L, "A"));
        Unit sectionB = unit(102L, group(12L, "Grade 7"), section(102L, "B"));

        TimetablePlanningData data = data(
                List.of(sectionA, sectionB),
                Map.of(101L, week(5, 8), 102L, Map.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        TeacherWorkloadDashboardResponse response = service.getDashboard(7L);

        assertTrue(response.isTargetAvailable());
        assertEquals(40, response.getBenchmarkWeeklyCapacity());
        assertEquals(32, response.getExpectedTarget());
        assertTrue(response.getWarnings().stream()
                .anyMatch(w -> "PARTIAL_SCHEDULE_PATTERN".equals(w.getCode())));
    }

    @Test
    void deduplicatesChoiceGroupAlternativesForTheSameTeacherAndUnit() {
        AcademicGroup group = group(11L, "Grade 6");
        Unit sectionA = unit(101L, group, section(101L, "A"));
        AcademicGroupSubjectMapping physics = mapping(201L, group, subject(301L, "Physics"), 10, null);
        AcademicGroupChoiceMapping choice = choice(401L, 10, "Science options");
        physics.setAcademicGroupChoiceMapping(choice);
        AcademicGroupSubjectMapping chemistry = mapping(202L, group, subject(302L, "Chemistry"), 10, choice);

        TimetablePlanningData data = data(
                List.of(sectionA),
                Map.of(101L, week(5, 8)),
                List.of(physics, chemistry),
                List.of(new Lesson(sectionA, physics, null, staff1),
                        new Lesson(sectionA, chemistry, null, staff1)),
                List.of(),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        Row row = rowFor(service.getDashboard(7L), staff1);

        assertEquals(10, row.getAssignedPeriods());
        assertEquals(1, row.getBreakdown().size());
        assertEquals("Physics / Chemistry", row.getBreakdown().get(0).getSubjectName());
        assertEquals(10, row.getBreakdown().get(0).getWeeklyPeriods());
    }

    @Test
    void totalsReconcileWithBreakdownAndFixedActivities() {
        AcademicGroup group = group(11L, "Grade 6");
        Unit sectionA = unit(101L, group, section(101L, "A"));
        AcademicGroupSubjectMapping math = mapping(201L, group, subject(301L, "Math"), 20, null);
        AcademicGroupSubjectMapping english = mapping(202L, group, subject(302L, "English"), 15, null);

        FixedActivity assembly = new FixedActivity();
        assembly.setFixedActivityId(501L);
        assembly.setName("Assembly");
        assembly.setSubject(subject(303L, "Assembly"));
        assembly.getSubject().setSubjectType(SubjectType.ACTIVITY);
        assembly.setStaff(staff2);
        assembly.setAppliesToAllGroups(true);
        assembly.setActive(true);
        assembly.setDays(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

        TimetablePlanningData data = data(
                List.of(sectionA),
                Map.of(101L, week(5, 8)),
                List.of(math, english),
                List.of(new Lesson(sectionA, math, null, staff1),
                        new Lesson(sectionA, english, null, staff2)),
                List.of(),
                List.of(assembly));
        when(planningDataLoader.load(7L)).thenReturn(data);

        TeacherWorkloadDashboardResponse response = service.getDashboard(7L);
        Row first = rowFor(response, staff1);
        Row second = rowFor(response, staff2);

        assertEquals(20, first.getAssignedPeriods());
        assertEquals(17, second.getAssignedPeriods());
        assertEquals(second.getAssignedPeriods(), second.getBreakdown().stream()
                .mapToInt(b -> b.getWeeklyPeriods()).sum());
        assertEquals(1, second.getBreakdown().stream()
                .filter(b -> "FIXED_ACTIVITY".equals(b.getSource())).count());
        assertEquals(3, response.getSummary().getTotalTeachers());
        assertEquals(3, response.getSummary().getBelowTarget());
        assertEquals(0, response.getSummary().getOverloaded());
    }

    @Test
    void countsDistinctUnassignedSubjectsAndSkipsCoveredChoiceAlternatives() {
        AcademicGroup group = group(11L, "Grade 6");
        Unit sectionA = unit(101L, group, section(101L, "A"));
        AcademicGroupSubjectMapping math = mapping(201L, group, subject(301L, "Math"), 8, null);
        AcademicGroupChoiceMapping choice = choice(401L, 10, "Science options");
        AcademicGroupSubjectMapping physics = mapping(202L, group, subject(302L, "Physics"), 10, choice);
        AcademicGroupSubjectMapping chemistry = mapping(203L, group, subject(303L, "Chemistry"), 10, choice);

        TimetablePlanningData data = data(
                List.of(sectionA),
                Map.of(101L, week(5, 8)),
                List.of(math, physics, chemistry),
                List.of(new Lesson(sectionA, physics, null, staff1)),
                List.of(new MissingTeacher(sectionA, math, MissingTeacherReason.NO_ALLOCATION, null),
                        new MissingTeacher(sectionA, chemistry, MissingTeacherReason.NO_ALLOCATION, null)),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        TeacherWorkloadDashboardResponse response = service.getDashboard(7L);

        assertEquals(1, response.getSummary().getUnassignedSubjects());
        assertEquals("Math", response.getUnassignedSubjects().get(0).getSubjectName());
    }

    @Test
    void scopesEveryQueryToTheRequestedAcademicYear() {
        TimetablePlanningData data = data(
                List.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        when(planningDataLoader.load(7L)).thenReturn(data);

        service.getDashboard(7L);

        verify(academicYearRepository).findById(7L);
        verify(planningDataLoader).load(7L);
    }

    @Test
    void enforcesViewPermissionBeforeLoadingWorkload() {
        doThrow(new AccessDeniedException("denied"))
                .when(accessGuard).requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);

        assertThrows(AccessDeniedException.class, () -> service.getDashboard(7L));
    }

    // ─── fixtures ────────────────────────────────────────────────────────────

    private Row rowFor(TeacherWorkloadDashboardResponse response, Staff staff) {
        return response.getRows().stream()
                .filter(row -> row.getStaffId().equals(staff.getStaffId()))
                .findFirst()
                .orElseThrow();
    }

    private TimetablePlanningData data(
            List<Unit> units,
            Map<Long, Map<DayOfWeek, List<TimetablePeriod>>> scheduleByUnit,
            List<AcademicGroupSubjectMapping> mappings,
            List<Lesson> lessons,
            List<MissingTeacher> missing,
            List<FixedActivity> fixedActivities) {
        return TimetablePlanningData.builder()
                .year(year)
                .units(units)
                .scheduleByUnit(scheduleByUnit)
                .mappings(mappings)
                .lessons(lessons)
                .missingTeachers(missing)
                .fixedActivities(fixedActivities)
                .versions(List.of())
                .build();
    }

    private Staff staff(long id, String first, String last, String code) {
        Staff staff = new Staff();
        staff.setStaffId(id);
        staff.setFirstName(first);
        staff.setLastName(last);
        staff.setStaffCode(code);
        staff.setEmail(code.toLowerCase() + "@school.test");
        staff.setActive(true);
        staff.setEmploymentStatus(EmploymentStatus.ACTIVE);
        staff.setStaffType(StaffType.TEACHING);
        return staff;
    }

    private AcademicGroup group(long id, String name) {
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(id);
        group.setName(name);
        group.setCode(name.replace(" ", ""));
        group.setActive(true);
        return group;
    }

    private AcademicSection section(long id, String name) {
        AcademicSection section = new AcademicSection();
        section.setSectionId(id);
        section.setName(name);
        section.setCode(name);
        section.setActive(true);
        return section;
    }

    private Unit unit(long sectionId, AcademicGroup group, AcademicSection section) {
        return new Unit(sectionId, section, group);
    }

    private Subject subject(long id, String name) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        subject.setCode("S" + id);
        subject.setSubjectType(SubjectType.ACADEMIC_SUBJECT);
        return subject;
    }

    private AcademicGroupSubjectMapping mapping(
            long id, AcademicGroup group, Subject subject, int weeklyPeriods,
            AcademicGroupChoiceMapping choice) {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroupSubjectMappingId(id);
        mapping.setAcademicGroup(group);
        mapping.setSubject(subject);
        mapping.setWeeklyPeriods((short) weeklyPeriods);
        mapping.setAcademicGroupChoiceMapping(choice);
        mapping.setActive(true);
        return mapping;
    }

    private AcademicGroupChoiceMapping choice(long id, int weeklyPeriods, String name) {
        SubjectChoiceGroup group = new SubjectChoiceGroup();
        group.setName(name);
        AcademicGroupChoiceMapping choice = new AcademicGroupChoiceMapping();
        choice.setAcademicGroupChoiceMappingId(id);
        choice.setWeeklyPeriods((short) weeklyPeriods);
        choice.setChoiceGroup(group);
        choice.setActive(true);
        return choice;
    }

    private Map<DayOfWeek, List<TimetablePeriod>> week(int days, int periodsPerDay) {
        Map<DayOfWeek, List<TimetablePeriod>> week = new LinkedHashMap<>();
        DayOfWeek[] values = DayOfWeek.values();
        int number = 1;
        for (int day = 0; day < days; day++) {
            List<TimetablePeriod> periods = new ArrayList<>();
            for (int period = 0; period < periodsPerDay; period++) {
                periods.add(period(number++, TimetableSlotKind.TEACHING));
            }
            week.put(values[day], periods);
        }
        return week;
    }

    private TimetablePeriod period(int number, TimetableSlotKind kind) {
        TimetablePeriod period = new TimetablePeriod();
        period.setTimetablePeriodId((long) number);
        period.setPeriodNumber((short) number);
        period.setName("Period " + number);
        period.setSlotKind(kind);
        return period;
    }
}
