package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.FixedActivityRequest;
import com.thinkerscave.academics.dto.request.FixedActivityResolveRequest;
import com.thinkerscave.academics.dto.response.FixedActivityOptionsResponse;
import com.thinkerscave.academics.dto.response.FixedActivityResponse;
import com.thinkerscave.academics.dto.response.FixedActivityScheduleResponse;
import com.thinkerscave.academics.dto.response.GroupAssignmentMatrixResponse;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableConfigurationServiceImplTest {

    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private AcademicGroupRepository academicGroupRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SchedulePatternRepository patternRepository;
    @Mock private TimetablePeriodRepository periodRepository;
    @Mock private GroupScheduleAssignmentRepository assignmentRepository;
    @Mock private FixedActivityRepository fixedActivityRepository;
    @Mock private FixedActivityPeriodRepository fixedActivityPeriodRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private AcademicGroupSubjectMappingRepository mappingRepository;
    @Mock private TeacherAllocationRepository teacherAllocationRepository;
    @Mock private TeacherAllocationTeacherRepository allocationTeacherRepository;
    @Mock private TeacherGroupAllocationRepository groupAllocationRepository;
    @Mock private TimetableEntryRepository entryRepository;
    @Mock private TimetableVersionRepository versionRepository;
    @Mock private TimetableConfigurationRepository configurationRepository;
    @Mock private TimetableWorkingDayRepository workingDayRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private AcademicsAccessGuard accessGuard;

    private TimetableConfigurationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TimetableConfigurationServiceImpl(
                academicYearRepository,
                academicGroupRepository,
                sectionRepository,
                patternRepository,
                periodRepository,
                assignmentRepository,
                fixedActivityRepository,
                fixedActivityPeriodRepository,
                subjectRepository,
                mappingRepository,
                teacherAllocationRepository,
                allocationTeacherRepository,
                groupAllocationRepository,
                entryRepository,
                versionRepository,
                configurationRepository,
                workingDayRepository,
                staffRepository,
                accessGuard);
    }

    @Test
    void exposesGroupWideAssignmentsAsAnAllSectionsMatrixRow() {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(1L);
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));

        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(3L);
        group.setName("Class 7");
        group.setCode("C7");
        group.setActive(true);
        group.setDisplayOrder(1);
        when(academicGroupRepository.findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(group));

        AcademicSection sectionA = section(group, 11L, "A");
        AcademicSection sectionB = section(group, 12L, "B");
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA, sectionB));

        SchedulePattern pattern = new SchedulePattern();
        pattern.setSchedulePatternId(5L);
        pattern.setName("Standard School Day");
        when(periodRepository.findBySchedulePattern_SchedulePatternIdOrderByPeriodNumberAsc(5L))
                .thenReturn(List.of());

        GroupScheduleAssignment assignment = new GroupScheduleAssignment();
        assignment.setGroupScheduleAssignmentId(21L);
        assignment.setAcademicYear(year);
        assignment.setAcademicGroup(group);
        assignment.setSchedulePattern(pattern);
        assignment.setDayOfWeek(DayOfWeek.MONDAY);
        assignment.setActive(true);
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(assignment));

        GroupAssignmentMatrixResponse matrix = service.getAssignmentMatrix(1L);

        assertEquals(3, matrix.getRows().size());
        GroupAssignmentMatrixResponse.Row allSections = matrix.getRows().get(0);
        assertEquals("All Sections", allSections.getScopeLabel());
        assertEquals(1, allSections.getCells().size());
        assertEquals(21L, allSections.getCells().get(0).getGroupScheduleAssignmentId());
        assertTrue(matrix.getRows().get(1).getCells().isEmpty());
        assertTrue(matrix.getRows().get(2).getCells().isEmpty());
    }

    @Test
    void rejectsPeriodThatIsMissingOnOneSelectedDayWithExactMessage() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");
        SchedulePattern saturday = pattern(6L, "Saturday");
        TimetablePeriod weekdaySix = period(weekday, 6, "Period 6", 11, 0);
        TimetablePeriod saturdayFive = period(saturday, 5, "Period 5", 11, 0);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY),
                assignment(year, group, null, saturday, DayOfWeek.SATURDAY)));
        when(periodRepository.findAllSlotsForYear(1L)).thenReturn(List.of(
                weekdaySix, period(weekday, 1, "Period 1", 9, 0),
                saturdayFive, period(saturday, 1, "Period 1", 9, 30)));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(activitySubject(10L)));

        FixedActivityRequest request = new FixedActivityRequest();
        request.setSubjectId(10L);
        request.setDays(new LinkedHashSet<>(List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)));
        request.setGroups(List.of(unit(3L, null)));
        request.setPeriodName("Period 6");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createFixedActivity(1L, request));

        assertEquals("Period 6 is not configured for Saturday for Class 9, Section A. "
                        + "Create separate fixed activity schedules for the applicable days or choose "
                        + "a period available on every selected day.",
                error.getMessage());
    }

    @Test
    void resolveOffersOnlyPeriodsSharedByEverySelectedDay() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");
        SchedulePattern saturday = pattern(6L, "Saturday");

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY),
                assignment(year, group, null, saturday, DayOfWeek.SATURDAY)));
        when(periodRepository.findAllSlotsForYear(1L)).thenReturn(List.of(
                period(weekday, 1, "Period 1", 9, 0),
                period(saturday, 1, "Period 1", 9, 30),
                period(weekday, 2, "Period 2", 10, 0),
                period(saturday, 2, "Period 2", 10, 30),
                period(weekday, 6, "Period 6", 15, 0)));

        FixedActivityResolveRequest request = new FixedActivityResolveRequest();
        request.setDays(new LinkedHashSet<>(List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY)));
        request.setGroups(List.of(resolveUnit(3L, null)));

        FixedActivityScheduleResponse response = service.resolveFixedActivitySchedule(1L, request);

        assertEquals(2, response.getCombinations().size());
        assertEquals(List.of("Period 1", "Period 2"),
                response.getCommonPeriods().stream().map(FixedActivityScheduleResponse.PeriodOption::getName)
                        .toList());
        assertEquals(2, response.getCommonPeriods().get(0).getTimes().size());
        assertTrue(response.getCommonPeriods().get(0).isTimesVary());
        assertTrue(response.getIssues().isEmpty());
    }

    @Test
    void savesAllSectionsAcademicFixedLessonWithResolvedTeacherAndGroupLevelScope() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");
        TimetablePeriod periodOne = period(weekday, 1, "Period 1", 9, 0);
        Subject subject = academicSubject(20L, "Mathematics");
        AcademicGroupSubjectMapping mapping = mapping(group, subject, 40L, 5);
        Staff teacher = staff(70L, "Asha", "Rao");
        TeacherAllocation allocation = allocation(mapping, sectionA, 50L);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY)));
        when(periodRepository.findAllSlotsForYear(1L)).thenReturn(List.of(periodOne));
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));
        when(mappingRepository.findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(3L, 20L))
                .thenReturn(Optional.of(mapping));
        when(teacherAllocationRepository
                .findByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(40L))
                .thenReturn(List.of(allocation));
        when(allocationTeacherRepository
                .findByTeacherAllocation_TeacherAllocationIdOrderByEffectiveFromDesc(50L))
                .thenReturn(List.of(allocationTeacher(allocation, teacher, 60L)));
        when(fixedActivityRepository.findDetailedByYear(1L)).thenReturn(List.of());
        when(fixedActivityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FixedActivityRequest request = fixedRequest(20L, DayOfWeek.MONDAY, "Period 1");

        FixedActivityResponse response = service.createFixedActivity(1L, request);

        assertEquals("ACADEMIC_SUBJECT", response.getSubjectType());
        assertEquals(70L, response.getStaffId());
        assertEquals("Asha Rao", response.getStaffName());
        assertTrue(response.getActive());
        assertEquals(1, response.getGroups().size());
        assertEquals(3L, response.getGroups().get(0).getAcademicGroupId());
        assertNull(response.getGroups().get(0).getSectionId(),
                "All Sections must persist as a group-level row so new sections follow it");
        assertEquals(1, response.getPeriods().size());
    }

    @Test
    void rejectsAcademicFixedLessonWithoutTeacherAllocation() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");
        Subject subject = academicSubject(20L, "Mathematics");
        AcademicGroupSubjectMapping mapping = mapping(group, subject, 40L, 5);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY)));
        when(periodRepository.findAllSlotsForYear(1L))
                .thenReturn(List.of(period(weekday, 1, "Period 1", 9, 0)));
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));
        when(mappingRepository.findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(3L, 20L))
                .thenReturn(Optional.of(mapping));
        when(teacherAllocationRepository
                .findByAcademicGroupSubjectMapping_AcademicGroupSubjectMappingId(40L))
                .thenReturn(List.of());

        FixedActivityRequest request = fixedRequest(20L, DayOfWeek.MONDAY, "Period 1");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createFixedActivity(1L, request));

        assertTrue(error.getMessage().contains("No teacher is allocated to Mathematics"));
        assertTrue(error.getMessage().contains("Teacher Allocation"));
    }

    @Test
    void rejectsAcademicFixedLessonThatExceedsWeeklyWorkload() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");
        Subject subject = academicSubject(20L, "Mathematics");
        AcademicGroupSubjectMapping mapping = mapping(group, subject, 40L, 1);

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY),
                assignment(year, group, null, weekday, DayOfWeek.TUESDAY)));
        when(periodRepository.findAllSlotsForYear(1L))
                .thenReturn(List.of(period(weekday, 1, "Period 1", 9, 0)));
        when(subjectRepository.findById(20L)).thenReturn(Optional.of(subject));
        when(mappingRepository.findByAcademicGroup_AcademicGroupIdAndSubject_SubjectId(3L, 20L))
                .thenReturn(Optional.of(mapping));

        FixedActivityRequest request = fixedRequest(20L, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, "Period 1");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createFixedActivity(1L, request));

        assertTrue(error.getMessage().contains("weekly workload is 1"));
    }

    @Test
    void activitySubjectSavesWithoutTeacherAllocation() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");
        SchedulePattern weekday = pattern(5L, "Weekday");

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicGroupRepository.findByIdWithYear(3L)).thenReturn(Optional.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));
        when(assignmentRepository.findDetailedByYear(1L)).thenReturn(List.of(
                assignment(year, group, null, weekday, DayOfWeek.MONDAY)));
        when(periodRepository.findAllSlotsForYear(1L))
                .thenReturn(List.of(period(weekday, 1, "Period 1", 9, 0)));
        when(subjectRepository.findById(10L)).thenReturn(Optional.of(activitySubject(10L)));
        when(fixedActivityRepository.findDetailedByYear(1L)).thenReturn(List.of());
        when(fixedActivityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FixedActivityRequest request = fixedRequest(10L, DayOfWeek.MONDAY, "Period 1");

        FixedActivityResponse response = service.createFixedActivity(1L, request);

        assertEquals("ACTIVITY", response.getSubjectType());
        assertNull(response.getStaffId());
        assertNull(response.getStaffName());
    }

    @Test
    void optionsIncludeAcademicAndActivitySubjectsWithTheirType() {
        AcademicYear year = year(1L);
        AcademicGroup group = group(year, 3L, "Class 9");
        AcademicSection sectionA = section(group, 11L, "A");

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(subjectRepository.findByActiveTrueOrderByNameAsc())
                .thenReturn(List.of(activitySubject(10L), academicSubject(20L, "Mathematics")));
        when(academicGroupRepository.findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(group));
        when(sectionRepository.findByAcademicGroup_AcademicGroupIdAndActiveTrueOrderByDisplayOrderAsc(3L))
                .thenReturn(List.of(sectionA));

        FixedActivityOptionsResponse response = service.getFixedActivityOptions(1L);

        assertEquals(List.of("ACADEMIC_SUBJECT", "ACTIVITY"),
                response.getSubjects().stream()
                        .map(FixedActivityOptionsResponse.SubjectOption::getSubjectType).toList());
        assertEquals("Mathematics", response.getSubjects().get(0).getName());
        assertEquals(1, response.getGroups().size());
        assertEquals(1, response.getGroups().get(0).getSections().size());
    }

    @Test
    void deactivationIsBlockedWhileALiveTimetableExists() {
        AcademicYear year = year(1L);
        FixedActivity activity = new FixedActivity();
        activity.setFixedActivityId(90L);
        activity.setAcademicYear(year);
        activity.setActive(true);
        TimetableVersion version = new TimetableVersion();
        version.setStatus(TimetableStatus.PUBLISHED);

        when(fixedActivityRepository.findDetailedById(90L)).thenReturn(Optional.of(activity));
        when(versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(1L))
                .thenReturn(List.of(version));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.setFixedActivityActive(90L, false));

        assertTrue(error.getMessage().contains("cannot be deactivated"));
        assertTrue(activity.getActive(), "The guard must not change the stored status");
    }

    @Test
    void deletionIsBlockedWhenAGeneratedTimetableReferencesTheActivity() {
        AcademicYear year = year(1L);
        FixedActivity activity = new FixedActivity();
        activity.setFixedActivityId(90L);
        activity.setAcademicYear(year);

        when(fixedActivityRepository.findDetailedById(90L)).thenReturn(Optional.of(activity));
        when(entryRepository.existsByFixedActivity_FixedActivityId(90L)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deleteFixedActivity(90L));

        assertTrue(error.getMessage().contains("referenced by a generated timetable"));
    }

    private FixedActivityRequest fixedRequest(Long subjectId, DayOfWeek day, String periodName) {
        return fixedRequest(subjectId, day, null, periodName);
    }

    private FixedActivityRequest fixedRequest(Long subjectId, DayOfWeek firstDay, DayOfWeek secondDay,
                                              String periodName) {
        FixedActivityRequest request = new FixedActivityRequest();
        request.setSubjectId(subjectId);
        java.util.Set<DayOfWeek> days = new LinkedHashSet<>();
        days.add(firstDay);
        if (secondDay != null) days.add(secondDay);
        request.setDays(days);
        request.setGroups(List.of(unit(3L, null)));
        request.setPeriodName(periodName);
        return request;
    }

    private Subject academicSubject(Long id, String name) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        subject.setCode("MATH");
        subject.setSubjectType(SubjectType.ACADEMIC_SUBJECT);
        subject.setActive(true);
        return subject;
    }

    private AcademicGroupSubjectMapping mapping(AcademicGroup group, Subject subject, Long id, int weekly) {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroupSubjectMappingId(id);
        mapping.setAcademicGroup(group);
        mapping.setSubject(subject);
        mapping.setWeeklyPeriods((short) weekly);
        mapping.setActive(true);
        return mapping;
    }

    private Staff staff(Long id, String first, String last) {
        Staff staff = new Staff();
        staff.setStaffId(id);
        staff.setFirstName(first);
        staff.setLastName(last);
        staff.setActive(true);
        return staff;
    }

    private TeacherAllocation allocation(AcademicGroupSubjectMapping mapping, AcademicSection section, Long id) {
        TeacherAllocation allocation = new TeacherAllocation();
        allocation.setTeacherAllocationId(id);
        allocation.setAcademicGroupSubjectMapping(mapping);
        allocation.setSection(section);
        allocation.setActive(true);
        return allocation;
    }

    private TeacherAllocationTeacher allocationTeacher(TeacherAllocation allocation, Staff staff, Long id) {
        TeacherAllocationTeacher teacher = new TeacherAllocationTeacher();
        teacher.setTeacherAllocationTeacherId(id);
        teacher.setTeacherAllocation(allocation);
        teacher.setStaff(staff);
        teacher.setRole(TeacherAllocationTeacherRole.PRIMARY);
        teacher.setEffectiveFrom(java.time.LocalDate.now().minusDays(1));
        teacher.setActive(true);
        return teacher;
    }

    private AcademicSection section(AcademicGroup group, Long id, String name) {
        AcademicSection section = new AcademicSection();
        section.setSectionId(id);
        section.setAcademicGroup(group);
        section.setName(name);
        section.setCode(name);
        section.setActive(true);
        return section;
    }

    private AcademicYear year(Long id) {
        AcademicYear year = new AcademicYear();
        year.setAcademicYearId(id);
        return year;
    }

    private AcademicGroup group(AcademicYear year, Long id, String name) {
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(id);
        group.setAcademicYear(year);
        group.setName(name);
        group.setCode("C" + id);
        group.setActive(true);
        group.setDisplayOrder(1);
        return group;
    }

    private Subject activitySubject(Long id) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName("Assembly");
        subject.setCode("ACT");
        subject.setSubjectType(SubjectType.ACTIVITY);
        subject.setActive(true);
        return subject;
    }

    private SchedulePattern pattern(Long id, String name) {
        SchedulePattern pattern = new SchedulePattern();
        pattern.setSchedulePatternId(id);
        pattern.setName(name);
        pattern.setActive(true);
        return pattern;
    }

    private TimetablePeriod period(SchedulePattern pattern, int number, String name, int hour, int minute) {
        TimetablePeriod period = new TimetablePeriod();
        LocalTime start = LocalTime.of(hour, minute);
        period.setSchedulePattern(pattern);
        period.setPeriodNumber((short) number);
        period.setName(name);
        period.setStartTime(start);
        period.setEndTime(start.plusMinutes(30));
        period.setSlotKind(TimetableSlotKind.TEACHING);
        return period;
    }

    private GroupScheduleAssignment assignment(AcademicYear year, AcademicGroup group, AcademicSection section,
                                               SchedulePattern pattern, DayOfWeek day) {
        GroupScheduleAssignment assignment = new GroupScheduleAssignment();
        assignment.setAcademicYear(year);
        assignment.setAcademicGroup(group);
        assignment.setSection(section);
        assignment.setSchedulePattern(pattern);
        assignment.setDayOfWeek(day);
        assignment.setActive(true);
        return assignment;
    }

    private FixedActivityRequest.Unit unit(Long groupId, Long sectionId) {
        FixedActivityRequest.Unit unit = new FixedActivityRequest.Unit();
        unit.setAcademicGroupId(groupId);
        unit.setSectionId(sectionId);
        return unit;
    }

    private FixedActivityResolveRequest.Unit resolveUnit(Long groupId, Long sectionId) {
        FixedActivityResolveRequest.Unit unit = new FixedActivityResolveRequest.Unit();
        unit.setAcademicGroupId(groupId);
        unit.setSectionId(sectionId);
        return unit;
    }
}
