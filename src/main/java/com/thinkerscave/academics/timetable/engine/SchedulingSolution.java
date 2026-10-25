package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;

import java.util.*;

/**
 * Mutable schedule with O(1) occupancy lookups for sections, teachers and rooms.
 */
@Getter
public class SchedulingSolution {

    private record SlotKey(Long ownerId, DayOfWeek day, Long periodId) {}

    private record SubjectDayKey(Long sectionId, Long mappingId, DayOfWeek day) {}

    private record TeacherDayKey(Long staffId, DayOfWeek day) {}

    private final Set<Placement> placements = new LinkedHashSet<>();
    private final List<SchedulingRequirement> unplacedRequirements = new ArrayList<>();

    private final Map<SlotKey, Placement> sectionSlots = new HashMap<>();
    private final Map<SlotKey, Placement> teacherSlots = new HashMap<>();
    private final Map<SlotKey, Placement> resourceSlots = new HashMap<>();
    private final Map<Long, Integer> teacherWeeklyCount = new HashMap<>();
    private final Map<TeacherDayKey, Integer> teacherDayCount = new HashMap<>();
    private final Map<SubjectDayKey, List<Short>> subjectDayPeriods = new HashMap<>();

    public void place(Placement p) {
        placements.add(p);
        sectionSlots.put(new SlotKey(p.getSectionId(), p.getDayOfWeek(), p.getPeriodId()), p);
        teacherSlots.put(new SlotKey(p.getStaffId(), p.getDayOfWeek(), p.getPeriodId()), p);
        if (p.getResourceId() != null) {
            resourceSlots.put(new SlotKey(p.getResourceId(), p.getDayOfWeek(), p.getPeriodId()), p);
        }
        teacherWeeklyCount.merge(p.getStaffId(), 1, Integer::sum);
        teacherDayCount.merge(new TeacherDayKey(p.getStaffId(), p.getDayOfWeek()), 1, Integer::sum);
        subjectDayPeriods.computeIfAbsent(
                new SubjectDayKey(p.getSectionId(), p.getAcademicGroupSubjectMappingId(), p.getDayOfWeek()),
                k -> new ArrayList<>()).add(p.getPeriodNumber());
    }

    public void unplace(Placement p) {
        if (!placements.remove(p)) return;
        sectionSlots.remove(new SlotKey(p.getSectionId(), p.getDayOfWeek(), p.getPeriodId()), p);
        teacherSlots.remove(new SlotKey(p.getStaffId(), p.getDayOfWeek(), p.getPeriodId()), p);
        if (p.getResourceId() != null) {
            resourceSlots.remove(new SlotKey(p.getResourceId(), p.getDayOfWeek(), p.getPeriodId()), p);
        }
        teacherWeeklyCount.merge(p.getStaffId(), -1, Integer::sum);
        teacherDayCount.merge(new TeacherDayKey(p.getStaffId(), p.getDayOfWeek()), -1, Integer::sum);
        List<Short> periods = subjectDayPeriods.get(
                new SubjectDayKey(p.getSectionId(), p.getAcademicGroupSubjectMappingId(), p.getDayOfWeek()));
        if (periods != null) {
            periods.remove(Short.valueOf(p.getPeriodNumber()));
        }
    }

    public boolean isSectionOccupied(Long sectionId, DayOfWeek day, Long periodId) {
        return sectionSlots.containsKey(new SlotKey(sectionId, day, periodId));
    }

    public boolean isTeacherOccupied(Long staffId, DayOfWeek day, Long periodId) {
        return teacherSlots.containsKey(new SlotKey(staffId, day, periodId));
    }

    public boolean isResourceOccupied(Long resourceId, DayOfWeek day, Long periodId) {
        return resourceId != null && resourceSlots.containsKey(new SlotKey(resourceId, day, periodId));
    }

    public Placement placementAtSection(Long sectionId, DayOfWeek day, Long periodId) {
        return sectionSlots.get(new SlotKey(sectionId, day, periodId));
    }

    public Placement placementAtTeacher(Long staffId, DayOfWeek day, Long periodId) {
        return teacherSlots.get(new SlotKey(staffId, day, periodId));
    }

    public Placement placementAtResource(Long resourceId, DayOfWeek day, Long periodId) {
        return resourceId == null ? null : resourceSlots.get(new SlotKey(resourceId, day, periodId));
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

    public int countPlacementsForSubjectInDay(Long sectionId, Long mappingId, DayOfWeek day) {
        List<Short> periods = subjectDayPeriods.get(new SubjectDayKey(sectionId, mappingId, day));
        return periods == null ? 0 : periods.size();
    }

    public boolean hasConsecutiveSubject(Long sectionId, Long mappingId, DayOfWeek day, short periodNumber) {
        List<Short> periods = subjectDayPeriods.get(new SubjectDayKey(sectionId, mappingId, day));
        if (periods == null) return false;
        for (Short existing : periods) {
            if (Math.abs(existing - periodNumber) == 1) return true;
        }
        return false;
    }
}
