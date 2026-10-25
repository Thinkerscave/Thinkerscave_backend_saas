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
    private final SectionRepository sectionRepository;
    private final AcademicGroupSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionRepository versionRepository;
    private final AcademicResourceRepository resourceRepository;

    /**
     * Picks the configuration timetable generation uses when a year has more than one active
     * configuration: the REGULAR shift first, then the oldest configuration.
     */
    public static TimetableConfiguration selectActive(List<TimetableConfiguration> configurations) {
        return configurations.stream()
                .filter(TimetableConfiguration::isActive)
                .min(Comparator
                        .comparing((TimetableConfiguration A) -> A.getShiftType() == TimetableShiftType.REGULAR ? 0 : 1)
                        .thenComparing(TimetableConfiguration::getTimetableConfigurationId))
                .orElse(null);
    }

    public TimetableConfiguration resolveActiveConfiguration(Long yearId) {
        return selectActive(configurationRepository.findByAcademicYear_AcademicYearId(yearId));
    }

    public TimetablePlanningData load(Long yearId) {
        return load(yearId, null);
    }

    /**
     * @param configurationOverride configuration to plan against (e.g. the one pinned on a
     *                              timetable version); {@code null} resolves the active one.
     */
    public TimetablePlanningData load(Long yearId, TimetableConfiguration configurationOverride) {
        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);

        List<TimetableConfiguration> activeConfigs = configurationRepository
                .findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(TimetableConfiguration::isActive)
                .sorted(Comparator.comparing(TimetableConfiguration::getTimetableConfigurationId))
                .toList();
        TimetableConfiguration config = configurationOverride != null
                ? configurationOverride : selectActive(activeConfigs);

        List<DayOfWeek> workingDays = List.of();
        List<TimetablePeriod> allPeriods = List.of();
        if (config != null) {
            Long configId = config.getTimetableConfigurationId();
            workingDays = workingDayRepository.findByTimetableConfiguration_TimetableConfigurationId(configId)
                    .stream()
                    .filter(w -> Boolean.TRUE.equals(w.getWorking()))
                    .map(TimetableWorkingDay::getDayOfWeek)
                    .sorted()
                    .toList();
            allPeriods = periodRepository
                    .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
        }
        List<TimetablePeriod> teachingPeriods = allPeriods.stream()
                .filter(p -> p.getSlotKind() == TimetableSlotKind.TEACHING)
                .toList();

        List<AcademicSection> sections = sectionRepository.findActiveForTimetableByYear(yearId);
        List<AcademicGroupSubjectMapping> mappings = mappingRepository.findActiveForTimetableByYear(yearId).stream()
                .sorted(Comparator
                        .comparing((AcademicGroupSubjectMapping m) -> m.getAcademicGroup().getDisplayOrder(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(m -> m.getSubject().getName()))
                .toList();

        Map<String, TeacherAllocation> allocationByKey = new HashMap<>();
        for (TeacherAllocation a : allocationRepository.findActiveWithDetailsByYear(yearId)) {
            allocationByKey.put(key(a.getSection().getSectionId(), a.getAcademicGroupSubjectMapping().getAcademicGroupSubjectMappingId()), a);
        }

        Map<Long, TeacherAllocationTeacher> primaryByAllocation = new HashMap<>();
        for (TeacherAllocationTeacher t : allocationTeacherRepository.findActiveByYear(yearId)) {
            if (t.getRole() != TeacherAllocationTeacherRole.PRIMARY) continue;
            Long allocationId = t.getTeacherAllocation().getTeacherAllocationId();
            primaryByAllocation.merge(allocationId, t, (a, b) ->
                    effectiveFrom(b).isAfter(effectiveFrom(a)) ? b : a);
        }

        Map<Long, List<AcademicGroupSubjectMapping>> mappingsByClass = new HashMap<>();
        List<AcademicGroupSubjectMapping> zeroPeriodMappings = new ArrayList<>();
        for (AcademicGroupSubjectMapping m : mappings) {
            if (m.getWeeklyPeriods() == null || m.getWeeklyPeriods() <= 0) {
                zeroPeriodMappings.add(m);
                continue;
            }
            mappingsByClass.computeIfAbsent(m.getAcademicGroup().getAcademicGroupId(), k -> new ArrayList<>()).add(m);
        }

        List<TimetablePlanningData.Lesson> lessons = new ArrayList<>();
        List<TimetablePlanningData.MissingTeacher> missing = new ArrayList<>();
        for (AcademicSection section : sections) {
            for (AcademicGroupSubjectMapping mapping : mappingsByClass.getOrDefault(
                    section.getAcademicGroup().getAcademicGroupId(), List.of())) {
                TeacherAllocation allocation = allocationByKey.get(
                        key(section.getSectionId(), mapping.getAcademicGroupSubjectMappingId()));
                if (allocation == null) {
                    missing.add(new TimetablePlanningData.MissingTeacher(section, mapping,
                            TimetablePlanningData.MissingTeacherReason.NO_ALLOCATION, null));
                    continue;
                }
                if (allocation.getStatus() != TeacherAllocationStatus.ASSIGNED) {
                    missing.add(new TimetablePlanningData.MissingTeacher(section, mapping,
                            TimetablePlanningData.MissingTeacherReason.UNASSIGNED, null));
                    continue;
                }
                TeacherAllocationTeacher primary = primaryByAllocation.get(allocation.getTeacherAllocationId());
                if (primary == null || primary.getStaff() == null) {
                    missing.add(new TimetablePlanningData.MissingTeacher(section, mapping,
                            TimetablePlanningData.MissingTeacherReason.NO_PRIMARY_TEACHER, null));
                    continue;
                }
                Staff staff = primary.getStaff();
                if (!isStaffAvailable(staff)) {
                    missing.add(new TimetablePlanningData.MissingTeacher(section, mapping,
                            TimetablePlanningData.MissingTeacherReason.TEACHER_INACTIVE,
                            TimetablePlanningData.staffName(staff)));
                    continue;
                }
                lessons.add(new TimetablePlanningData.Lesson(section, mapping, allocation, staff));
            }
        }

        List<TimetableVersion> versions = versionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);
        List<AcademicResource> resources = resourceRepository.findByActiveTrueOrderByNameAsc();

        return TimetablePlanningData.builder()
                .year(year)
                .configuration(config)
                .activeConfigurations(activeConfigs)
                .workingDays(workingDays)
                .allPeriods(allPeriods)
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
