package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;

import java.util.*;

@Getter
public class SchedulingSolution {

    private final List<Placement> placements = new ArrayList<>();
    private final List<SchedulingRequirement> unplacedRequirements = new ArrayList<>();

    private final Map<Long, Map<DayOfWeek, Set<Long>>> sectionOccupancy = new HashMap<>();
    private final Map<Long, Map<DayOfWeek, Set<Long>>> teacherOccupancy = new HashMap<>();
    private final Map<Long, Map<DayOfWeek, Set<Long>>> resourceOccupancy = new HashMap<>();
    private final Map<Long, Integer> teacherWeeklyCount = new HashMap<>();

    public void place(Placement p) {
        placements.add(p);
        occupy(sectionOccupancy, p.getSectionId(), p.getDayOfWeek(), p.getPeriodId());
        occupy(teacherOccupancy, p.getStaffId(), p.getDayOfWeek(), p.getPeriodId());
        if (p.getResourceId() != null) {
            occupy(resourceOccupancy, p.getResourceId(), p.getDayOfWeek(), p.getPeriodId());
        }
        teacherWeeklyCount.merge(p.getStaffId(), 1, Integer::sum);
    }

    public void unplace(Placement p) {
        placements.remove(p);
        vacate(sectionOccupancy, p.getSectionId(), p.getDayOfWeek(), p.getPeriodId());
        vacate(teacherOccupancy, p.getStaffId(), p.getDayOfWeek(), p.getPeriodId());
        if (p.getResourceId() != null) {
            vacate(resourceOccupancy, p.getResourceId(), p.getDayOfWeek(), p.getPeriodId());
        }
        teacherWeeklyCount.merge(p.getStaffId(), -1, Integer::sum);
    }

    public boolean isSectionOccupied(Long sectionId, DayOfWeek day, Long periodId) {
        return isOccupied(sectionOccupancy, sectionId, day, periodId);
    }

    public boolean isTeacherOccupied(Long staffId, DayOfWeek day, Long periodId) {
        return isOccupied(teacherOccupancy, staffId, day, periodId);
    }

    public boolean isResourceOccupied(Long resourceId, DayOfWeek day, Long periodId) {
        return resourceId != null && isOccupied(resourceOccupancy, resourceId, day, periodId);
    }

    public int getTeacherWeeklyLoad(Long staffId) {
        return teacherWeeklyCount.getOrDefault(staffId, 0);
    }

    public boolean isComplete() {
        return unplacedRequirements.isEmpty();
    }

    public int countPlacementsForSubjectInDay(Long sectionId, Long mappingId, DayOfWeek day) {
        int count = 0;
        for (Placement p : placements) {
            if (p.getSectionId().equals(sectionId)
                    && p.getClassSubjectMappingId().equals(mappingId)
                    && p.getDayOfWeek() == day) {
                count++;
            }
        }
        return count;
    }

    public boolean hasConsecutiveSubject(Long sectionId, Long mappingId, DayOfWeek day, short periodNumber) {
        for (Placement p : placements) {
            if (p.getSectionId().equals(sectionId)
                    && p.getClassSubjectMappingId().equals(mappingId)
                    && p.getDayOfWeek() == day
                    && Math.abs(p.getPeriodNumber() - periodNumber) == 1) {
                return true;
            }
        }
        return false;
    }

    public int getTeacherDayLoad(Long staffId, DayOfWeek day) {
        int count = 0;
        for (Placement p : placements) {
            if (p.getStaffId().equals(staffId) && p.getDayOfWeek() == day) {
                count++;
            }
        }
        return count;
    }

    private void occupy(Map<Long, Map<DayOfWeek, Set<Long>>> map, Long id, DayOfWeek day, Long periodId) {
        map.computeIfAbsent(id, k -> new EnumMap<>(DayOfWeek.class))
                .computeIfAbsent(day, k -> new HashSet<>())
                .add(periodId);
    }

    private void vacate(Map<Long, Map<DayOfWeek, Set<Long>>> map, Long id, DayOfWeek day, Long periodId) {
        Map<DayOfWeek, Set<Long>> dayMap = map.get(id);
        if (dayMap != null) {
            Set<Long> periods = dayMap.get(day);
            if (periods != null) {
                periods.remove(periodId);
            }
        }
    }

    private boolean isOccupied(Map<Long, Map<DayOfWeek, Set<Long>>> map, Long id, DayOfWeek day, Long periodId) {
        Map<DayOfWeek, Set<Long>> dayMap = map.get(id);
        if (dayMap == null) return false;
        Set<Long> periods = dayMap.get(day);
        return periods != null && periods.contains(periodId);
    }
}
