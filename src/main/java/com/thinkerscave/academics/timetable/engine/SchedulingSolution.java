package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;

import java.time.LocalTime;
import java.util.*;

/**
 * Mutable schedule with occupancy lookups for units, teachers and rooms. Occupancy is
 * interval-based so schedules from different time windows (morning/evening shifts) do not
 * create false clashes, while genuinely overlapping lessons across groups still do.
 */
@Getter
public class SchedulingSolution {

    private record SubjectDayKey(Long unitId, Long mappingId, DayOfWeek day) {}

    private record TeacherDayKey(Long staffId, DayOfWeek day) {}

    private final Set<Placement> placements = new LinkedHashSet<>();
    private final List<SchedulingRequirement> unplacedRequirements = new ArrayList<>();

    private final Map<String, List<Placement>> unitDay = new HashMap<>();
    private final Map<String, List<Placement>> teacherDay = new HashMap<>();
    private final Map<String, List<Placement>> resourceDay = new HashMap<>();
    private final Map<Long, Integer> teacherWeeklyCount = new HashMap<>();
    private final Map<TeacherDayKey, Integer> teacherDayCount = new HashMap<>();
    private final Map<SubjectDayKey, List<Short>> subjectDayPeriods = new HashMap<>();

    private static String key(Long ownerId, DayOfWeek day) {
        return ownerId + "|" + day;
    }

    public void place(Placement p) {
        placements.add(p);
        unitDay.computeIfAbsent(key(p.getUnitId(), p.getDayOfWeek()), k -> new ArrayList<>()).add(p);
        if (p.getStaffId() != null) {
            teacherDay.computeIfAbsent(key(p.getStaffId(), p.getDayOfWeek()), k -> new ArrayList<>()).add(p);
            teacherWeeklyCount.merge(p.getStaffId(), 1, Integer::sum);
            teacherDayCount.merge(new TeacherDayKey(p.getStaffId(), p.getDayOfWeek()), 1, Integer::sum);
        }
        if (p.getResourceId() != null) {
            resourceDay.computeIfAbsent(key(p.getResourceId(), p.getDayOfWeek()), k -> new ArrayList<>()).add(p);
        }
        if (p.getAcademicGroupSubjectMappingId() != null) {
            subjectDayPeriods.computeIfAbsent(
                    new SubjectDayKey(p.getUnitId(), p.getAcademicGroupSubjectMappingId(), p.getDayOfWeek()),
                    k -> new ArrayList<>()).add(p.getPeriodNumber());
        }
    }

    public void unplace(Placement p) {
        if (!placements.remove(p)) return;
        remove(unitDay, key(p.getUnitId(), p.getDayOfWeek()), p);
        if (p.getStaffId() != null) {
            remove(teacherDay, key(p.getStaffId(), p.getDayOfWeek()), p);
            teacherWeeklyCount.merge(p.getStaffId(), -1, Integer::sum);
            teacherDayCount.merge(new TeacherDayKey(p.getStaffId(), p.getDayOfWeek()), -1, Integer::sum);
        }
        if (p.getResourceId() != null) {
            remove(resourceDay, key(p.getResourceId(), p.getDayOfWeek()), p);
        }
        if (p.getAcademicGroupSubjectMappingId() != null) {
            List<Short> periods = subjectDayPeriods.get(
                    new SubjectDayKey(p.getUnitId(), p.getAcademicGroupSubjectMappingId(), p.getDayOfWeek()));
            if (periods != null) periods.remove(Short.valueOf(p.getPeriodNumber()));
        }
    }

    private static void remove(Map<String, List<Placement>> map, String key, Placement p) {
        List<Placement> list = map.get(key);
        if (list != null) list.remove(p);
    }

    private static boolean overlaps(Placement p, LocalTime start, LocalTime end) {
        int s1 = p.startMinute();
        int e1 = p.endMinute();
        int s2 = start == null ? 0 : start.getHour() * 60 + start.getMinute();
        int e2 = end == null ? 0 : end.getHour() * 60 + end.getMinute();
        return s1 < e2 && s2 < e1;
    }

    private static boolean overlapsAny(List<Placement> list, LocalTime start, LocalTime end) {
        if (list == null) return false;
        for (Placement p : list) {
            if (overlaps(p, start, end)) return true;
        }
        return false;
    }

    public boolean isSectionOccupied(Long unitId, DayOfWeek day, LocalTime start, LocalTime end) {
        return overlapsAny(unitDay.get(key(unitId, day)), start, end);
    }

    public boolean isSectionOccupiedFor(SchedulingRequirement requirement, DayOfWeek day,
                                        LocalTime start, LocalTime end) {
        List<Placement> list = unitDay.get(key(requirement.getUnitId(), day));
        if (list == null) return false;
        return list.stream().anyMatch(placement -> overlaps(placement, start, end)
                && !canShareChoiceSlot(placement, requirement));
    }

    public Placement placementAtSectionBlocking(SchedulingRequirement requirement, DayOfWeek day,
                                                Long periodId) {
        List<Placement> list = unitDay.get(key(requirement.getUnitId(), day));
        if (list == null) return null;
        return list.stream()
                .filter(placement -> Objects.equals(placement.getPeriodId(), periodId))
                .filter(placement -> !canShareChoiceSlot(placement, requirement))
                .findFirst().orElse(null);
    }

    public boolean choicePartnerAtDifferentSlot(SchedulingRequirement requirement, DayOfWeek day, Long periodId) {
        if (requirement.getChoiceMappingId() == null) return false;
        return unitDay.getOrDefault(key(requirement.getUnitId(), day), List.of()).stream()
                .anyMatch(placement -> Objects.equals(placement.getChoiceMappingId(), requirement.getChoiceMappingId())
                        && placement.getPeriodIndex() == requirement.getPeriodIndex()
                        && !Objects.equals(placement.getPeriodId(), periodId));
    }

    private boolean canShareChoiceSlot(Placement placement, SchedulingRequirement requirement) {
        return requirement.getChoiceMappingId() != null
                && Objects.equals(placement.getChoiceMappingId(), requirement.getChoiceMappingId())
                && placement.getPeriodIndex() == requirement.getPeriodIndex()
                && !Objects.equals(placement.getAcademicGroupSubjectMappingId(),
                requirement.getAcademicGroupSubjectMappingId())
                && !Objects.equals(placement.getSubjectId(), requirement.getSubjectId());
    }

    public boolean isTeacherOccupied(Long staffId, DayOfWeek day, LocalTime start, LocalTime end) {
        return overlapsAny(teacherDay.get(key(staffId, day)), start, end);
    }

    public boolean isResourceOccupied(Long resourceId, DayOfWeek day, LocalTime start, LocalTime end) {
        return resourceId != null && overlapsAny(resourceDay.get(key(resourceId, day)), start, end);
    }

    public boolean isSectionOccupied(Long unitId, DayOfWeek day, Long periodId) {
        List<Placement> list = unitDay.get(key(unitId, day));
        return list != null && list.stream().anyMatch(p -> Objects.equals(p.getPeriodId(), periodId));
    }

    public boolean isTeacherOccupied(Long staffId, DayOfWeek day, Long periodId) {
        List<Placement> list = teacherDay.get(key(staffId, day));
        return list != null && list.stream().anyMatch(p -> Objects.equals(p.getPeriodId(), periodId));
    }

    public boolean isResourceOccupied(Long resourceId, DayOfWeek day, Long periodId) {
        if (resourceId == null) return false;
        List<Placement> list = resourceDay.get(key(resourceId, day));
        return list != null && list.stream().anyMatch(p -> Objects.equals(p.getPeriodId(), periodId));
    }

    public Placement placementAtSection(Long unitId, DayOfWeek day, Long periodId) {
        List<Placement> list = unitDay.get(key(unitId, day));
        return list == null ? null : list.stream()
                .filter(p -> Objects.equals(p.getPeriodId(), periodId)).findFirst().orElse(null);
    }

    public Placement placementAtTeacher(Long staffId, DayOfWeek day, Long periodId) {
        List<Placement> list = teacherDay.get(key(staffId, day));
        return list == null ? null : list.stream()
                .filter(p -> Objects.equals(p.getPeriodId(), periodId)).findFirst().orElse(null);
    }

    public Placement placementAtResource(Long resourceId, DayOfWeek day, Long periodId) {
        if (resourceId == null) return null;
        List<Placement> list = resourceDay.get(key(resourceId, day));
        return list == null ? null : list.stream()
                .filter(p -> Objects.equals(p.getPeriodId(), periodId)).findFirst().orElse(null);
    }

    public int getTeacherWeeklyLoad(Long staffId) {
        return teacherWeeklyCount.getOrDefault(staffId, 0);
    }

    public int getTeacherDayLoad(Long staffId, DayOfWeek day) {
        return teacherDayCount.getOrDefault(new TeacherDayKey(staffId, day), 0);
    }

    public boolean isComplete() {
        return unplacedRequirements.isEmpty();
    }

    public int countPlacementsForSubjectInDay(Long unitId, Long mappingId, DayOfWeek day) {
        List<Short> periods = subjectDayPeriods.get(new SubjectDayKey(unitId, mappingId, day));
        return periods == null ? 0 : periods.size();
    }

    public boolean hasConsecutiveSubject(Long unitId, Long mappingId, DayOfWeek day, short periodNumber) {
        List<Short> periods = subjectDayPeriods.get(new SubjectDayKey(unitId, mappingId, day));
        if (periods == null) return false;
        for (Short existing : periods) {
            if (Math.abs(existing - periodNumber) == 1) return true;
        }
        return false;
    }
}
