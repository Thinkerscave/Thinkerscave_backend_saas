package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads {@link TimetablePlanningData}. Must be called inside a transaction because it walks
 * lazy associations of the loaded entities.
 */
@Component
@RequiredArgsConstructor
public class TimetablePlanningDataLoader {

    private static final Set<EmploymentStatus> INACTIVE_EMPLOYMENT = EnumSet.of(
            EmploymentStatus.RESIGNED,
            EmploymentStatus.RETIRED,
            EmploymentStatus.CONTRACT_COMPLETED
    );

    private final AcademicYearRepository academicYearRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SectionRepository sectionRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TeacherGroupAllocationRepository groupAllocationRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionRepository versionRepository;
    private final AcademicResourceRepository resourceRepository;
    private final SchedulePatternRepository patternRepository;
    private final GroupScheduleAssignmentRepository assignmentRepository;
    private final FixedActivityRepository fixedActivityRepository;

    public static TimetableConfiguration selectActive(List<TimetableConfiguration> configurations) {
        return configurations.stream()
                .filter(TimetableConfiguration::isActive)
                .min(Comparator
                        .comparing((TimetableConfiguration A) ->
                                A.getShiftType() == TimetableShiftType.REGULAR ? 0 : 1)
                        .thenComparing(TimetableConfiguration::getTimetableConfigurationId))
                .orElse(null);
    }

    public TimetableConfiguration resolveActiveConfiguration(Long yearId) {
        return selectActive(configurationRepository.findByAcademicYear_AcademicYearId(yearId));
    }

    public TimetablePlanningData load(Long yearId) {
        return load(yearId, null);
    }

    public TimetablePlanningData load(Long yearId, TimetableConfiguration configurationOverride) {
        return load(yearId, configurationOverride, null);
    }

    /**
     * @param configurationOverride settings carrier to use; {@code null} resolves the active one.
     * @param referenceDate         date used to resolve effective pattern assignments and fixed
     *                              activities; {@code null} clamps today to the academic year.
     */
    public TimetablePlanningData load(Long yearId, TimetableConfiguration configurationOverride,
                                      LocalDate referenceDate) {
        return load(yearId, configurationOverride, referenceDate, null);
    }

    public TimetablePlanningData load(Long yearId, TimetableConfiguration configurationOverride,
                                      LocalDate referenceDate, java.util.Set<Long> requestedGroupIds) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);

        List<TimetableConfiguration> activeConfigs = configurationRepository
                .findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(TimetableConfiguration::isActive)
                .sorted(Comparator.comparing(TimetableConfiguration::getTimetableConfigurationId))
                .toList();
        TimetableConfiguration config = configurationOverride != null
                ? configurationOverride : selectActive(activeConfigs);

        LocalDate effectiveDate = resolveReferenceDate(year, referenceDate);

        List<SchedulePattern> patterns = patternRepository
                .findByAcademicYear_AcademicYearIdOrderByNameAsc(yearId);
        List<TimetablePeriod> allSlots = periodRepository.findAllSlotsForYear(yearId);
        Map<Long, List<TimetablePeriod>> slotsByPattern = allSlots.stream()
                .filter(s -> s.getSchedulePattern() != null)
                .collect(Collectors.groupingBy(s -> s.getSchedulePattern().getSchedulePatternId()));
        slotsByPattern.values().forEach(list -> list.sort(
                Comparator.comparing(TimetablePeriod::getStartTime)
                        .thenComparing(TimetablePeriod::getPeriodNumber)));

        List<GroupScheduleAssignment> assignments = assignmentRepository.findDetailedByYear(yearId).stream()
                .filter(assignment -> requestedGroupIds == null || requestedGroupIds.isEmpty()
                        || requestedGroupIds.contains(assignment.getAcademicGroup().getAcademicGroupId()))
                .toList();
        List<FixedActivity> fixedActivities = fixedActivityRepository.findDetailedByYear(yearId);

        List<AcademicGroup> groups = academicGroupRepository
                .findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId).stream()
                .filter(group -> requestedGroupIds == null || requestedGroupIds.isEmpty()
                        || requestedGroupIds.contains(group.getAcademicGroupId()))
                .toList();
        java.util.Set<Long> selectedGroupIds = groups.stream()
                .map(AcademicGroup::getAcademicGroupId).collect(java.util.stream.Collectors.toSet());
        List<AcademicSection> sections = sectionRepository.findActiveForTimetableByYear(yearId).stream()
                .filter(section -> selectedGroupIds.contains(section.getAcademicGroup().getAcademicGroupId()))
                .toList();

        List<TimetablePlanningData.Unit> units = new ArrayList<>();
        Map<Long, AcademicSection> sectionById = sections.stream()
                .collect(Collectors.toMap(AcademicSection::getSectionId, s -> s));
        for (AcademicGroup group : groups) {
            List<AcademicSection> groupSections = sections.stream()
                    .filter(s -> s.getAcademicGroup().getAcademicGroupId().equals(group.getAcademicGroupId()))
                    .toList();
            if (groupSections.isEmpty()) {
                // Group without sections: one group-level scheduling unit. Its teachers come
                // from TeacherGroupAllocation because section-level allocation is unavailable.
                units.add(new TimetablePlanningData.Unit(-group.getAcademicGroupId(), null, group));
            } else {
                groupSections.forEach(section ->
                        units.add(new TimetablePlanningData.Unit(section.getSectionId(), section, group)));
            }
        }

        Map<Long, Map<DayOfWeek, List<TimetablePeriod>>> scheduleByUnit = new HashMap<>();
        for (TimetablePlanningData.Unit unit : units) {
            Map<DayOfWeek, List<TimetablePeriod>> byDay = new LinkedHashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                List<GroupScheduleAssignment> dayAssignments = assignments.stream()
                        .filter(a -> a.isActive() && a.isEffectiveOn(effectiveDate))
                        .filter(a -> a.getAcademicGroup().getAcademicGroupId()
                                .equals(unit.academicGroupId()))
                        .filter(a -> a.getDayOfWeek() == day)
                        .toList();
                List<GroupScheduleAssignment> sectionSpecific = unit.section() == null
                        ? List.of()
                        : dayAssignments.stream()
                        .filter(a -> a.getSection() != null
                                && a.getSection().getSectionId().equals(unit.sectionId()))
                        .toList();
                List<GroupScheduleAssignment> selected = sectionSpecific.isEmpty()
                        ? dayAssignments.stream().filter(a -> a.getSection() == null).toList()
                        : sectionSpecific;
                List<TimetablePeriod> slots = new ArrayList<>();
                Set<Long> seen = new HashSet<>();
                for (GroupScheduleAssignment assignment : selected) {
                    List<TimetablePeriod> patternSlots = slotsByPattern.getOrDefault(
                            assignment.getSchedulePattern().getSchedulePatternId(), List.of());
                    for (TimetablePeriod slot : patternSlots) {
                        if (seen.add(slot.getTimetablePeriodId())) slots.add(slot);
                    }
                }
                slots.sort(Comparator.comparing(TimetablePeriod::getStartTime)
                        .thenComparing(TimetablePeriod::getPeriodNumber));
                if (!slots.isEmpty()) byDay.put(day, slots);
            }
            scheduleByUnit.put(unit.unitId(), byDay);
        }

        Set<DayOfWeek> workingDays = new TreeSet<>();
        Set<Long> teachingIds = new HashSet<>();
        List<TimetablePeriod> teachingPeriods = new ArrayList<>();
        List<TimetablePeriod> unionPeriods = new ArrayList<>();
        Set<Long> unionIds = new HashSet<>();
        for (Map<DayOfWeek, List<TimetablePeriod>> byDay : scheduleByUnit.values()) {
            for (Map.Entry<DayOfWeek, List<TimetablePeriod>> entry : byDay.entrySet()) {
                workingDays.add(entry.getKey());
                for (TimetablePeriod slot : entry.getValue()) {
                    if (unionIds.add(slot.getTimetablePeriodId())) unionPeriods.add(slot);
                    if (slot.getSlotKind() == TimetableSlotKind.TEACHING && teachingIds.add(slot.getTimetablePeriodId())) {
                        teachingPeriods.add(slot);
                    }
                }
            }
        }
        unionPeriods.sort(Comparator.comparing(TimetablePeriod::getStartTime)
                .thenComparing(TimetablePeriod::getPeriodNumber));
        teachingPeriods.sort(Comparator.comparing(TimetablePeriod::getStartTime)
                .thenComparing(TimetablePeriod::getPeriodNumber));

        List<AcademicGroupSubjectMapping> mappings = mappingRepository.findActiveForTimetableByYear(yearId)
                .stream()
                .filter(mapping -> selectedGroupIds.contains(mapping.getAcademicGroup().getAcademicGroupId()))
                .sorted(Comparator
                        .comparing((AcademicGroupSubjectMapping m) -> m.getAcademicGroup().getDisplayOrder(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(m -> m.getSubject().getName()))
                .toList();

        Map<String, TeacherAllocation> allocationByKey = new HashMap<>();
        for (TeacherAllocation a : allocationRepository.findActiveWithDetailsByYear(yearId)) {
            allocationByKey.put(key(a.getSection().getSectionId(),
                    a.getAcademicGroupSubjectMapping().getAcademicGroupSubjectMappingId()), a);
        }

        Map<Long, TeacherAllocationTeacher> primaryByAllocation = new HashMap<>();
        for (TeacherAllocationTeacher t : allocationTeacherRepository.findActiveByYear(yearId)) {
            if (t.getRole() != TeacherAllocationTeacherRole.PRIMARY) continue;
            Long allocationId = t.getTeacherAllocation().getTeacherAllocationId();
            primaryByAllocation.merge(allocationId, t, (a, b) ->
                    effectiveFrom(b).isAfter(effectiveFrom(a)) ? b : a);
        }

        Map<Long, TeacherGroupAllocation> groupAllocationByMapping = new HashMap<>();
        for (TeacherGroupAllocation allocation : groupAllocationRepository.findActiveByYear(yearId)) {
            groupAllocationByMapping.put(
                    allocation.getMapping().getAcademicGroupSubjectMappingId(), allocation);
        }

        Map<Long, List<AcademicGroupSubjectMapping>> mappingsByGroup = new HashMap<>();
        List<AcademicGroupSubjectMapping> zeroPeriodMappings = new ArrayList<>();
        for (AcademicGroupSubjectMapping m : mappings) {
            if (m.getWeeklyPeriods() == null || m.getWeeklyPeriods() <= 0) {
                zeroPeriodMappings.add(m);
                continue;
            }
            mappingsByGroup.computeIfAbsent(m.getAcademicGroup().getAcademicGroupId(),
                    k -> new ArrayList<>()).add(m);
        }

        List<TimetablePlanningData.Lesson> lessons = new ArrayList<>();
        List<TimetablePlanningData.MissingTeacher> missing = new ArrayList<>();
        for (TimetablePlanningData.Unit unit : units) {
            for (AcademicGroupSubjectMapping mapping : mappingsByGroup.getOrDefault(
                    unit.academicGroupId(), List.of())) {
                if (unit.section() == null) {
                    TeacherGroupAllocation groupAllocation = groupAllocationByMapping.get(
                            mapping.getAcademicGroupSubjectMappingId());
                    if (groupAllocation == null || groupAllocation.getStaff() == null) {
                        missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                                TimetablePlanningData.MissingTeacherReason.NO_ALLOCATION, null));
                        continue;
                    }
                    Staff staff = groupAllocation.getStaff();
                    if (!isStaffAvailable(staff)) {
                        missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                                TimetablePlanningData.MissingTeacherReason.TEACHER_INACTIVE,
                                TimetablePlanningData.staffName(staff)));
                        continue;
                    }
                    lessons.add(new TimetablePlanningData.Lesson(unit, mapping, null, staff));
                    continue;
                }
                TeacherAllocation allocation = allocationByKey.get(
                        key(unit.sectionId(), mapping.getAcademicGroupSubjectMappingId()));
                if (allocation == null) {
                    missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                            TimetablePlanningData.MissingTeacherReason.NO_ALLOCATION, null));
                    continue;
                }
                if (allocation.getStatus() != TeacherAllocationStatus.ASSIGNED) {
                    missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                            TimetablePlanningData.MissingTeacherReason.UNASSIGNED, null));
                    continue;
                }
                TeacherAllocationTeacher primary = primaryByAllocation.get(allocation.getTeacherAllocationId());
                if (primary == null || primary.getStaff() == null) {
                    missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                            TimetablePlanningData.MissingTeacherReason.NO_PRIMARY_TEACHER, null));
                    continue;
                }
                Staff staff = primary.getStaff();
                if (!isStaffAvailable(staff)) {
                    missing.add(new TimetablePlanningData.MissingTeacher(unit, mapping,
                            TimetablePlanningData.MissingTeacherReason.TEACHER_INACTIVE,
                            TimetablePlanningData.staffName(staff)));
                    continue;
                }
                lessons.add(new TimetablePlanningData.Lesson(unit, mapping, allocation, staff));
            }
        }

        List<TimetableVersion> versions = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);
        List<AcademicResource> resources = resourceRepository.findByActiveTrueOrderByNameAsc();

        return TimetablePlanningData.builder()
                .year(year)
                .configuration(config)
                .patterns(patterns)
                .slotsByPattern(slotsByPattern)
                .assignments(assignments)
                .fixedActivities(fixedActivities)
                .units(units)
                .scheduleByUnit(scheduleByUnit)
                .referenceDate(effectiveDate)
                .workingDays(new ArrayList<>(workingDays))
                .allPeriods(unionPeriods)
                .teachingPeriods(teachingPeriods)
                .sections(sections)
                .mappings(mappings)
                .lessons(lessons)
                .missingTeachers(missing)
                .zeroPeriodMappings(zeroPeriodMappings)
                .resources(resources)
                .versions(versions)
                .build();
    }

    public static LocalDate resolveReferenceDate(AcademicYear year, LocalDate requested) {
        if (requested != null) {
            if (year == null) return requested;
            if (year.getStartDate() != null && requested.isBefore(year.getStartDate())) return year.getStartDate();
            if (year.getEndDate() != null && requested.isAfter(year.getEndDate())) return year.getEndDate();
            return requested;
        }
        LocalDate today = LocalDate.now();
        if (year == null) return today;
        if (year.getStartDate() != null && today.isBefore(year.getStartDate())) return year.getStartDate();
        if (year.getEndDate() != null && today.isAfter(year.getEndDate())) return year.getEndDate();
        return today;
    }

    private static boolean isStaffAvailable(Staff staff) {
        if (Boolean.FALSE.equals(staff.getActive())) return false;
        return staff.getEmploymentStatus() == null || !INACTIVE_EMPLOYMENT.contains(staff.getEmploymentStatus());
    }

    private static LocalDate effectiveFrom(TeacherAllocationTeacher t) {
        return t.getEffectiveFrom() == null ? LocalDate.MIN : t.getEffectiveFrom();
    }

    private static String key(Long sectionId, Long mappingId) {
        return sectionId + ":" + mappingId;
    }
}
