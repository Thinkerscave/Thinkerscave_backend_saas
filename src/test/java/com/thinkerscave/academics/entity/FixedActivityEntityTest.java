package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the natural-key equality of fixed activity children: a brand-new child row
 * has a null id, so id-only equality would collapse every unit/day into a single row.
 */
class FixedActivityEntityTest {

    @Test
    void newGroupAndPeriodRowsKeepEveryDistinctUnitAndDay() {
        FixedActivity activity = new FixedActivity();
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(1L);
        AcademicSection sectionA = new AcademicSection();
        sectionA.setSectionId(10L);
        AcademicSection sectionB = new AcademicSection();
        sectionB.setSectionId(11L);

        for (AcademicSection section : List.of(sectionA, sectionB)) {
            FixedActivityGroup link = new FixedActivityGroup();
            link.setFixedActivity(activity);
            link.setAcademicGroup(group);
            link.setSection(section);
            activity.getGroups().add(link);
        }
        assertEquals(2, activity.getGroups().size());

        TimetablePeriod period = new TimetablePeriod();
        period.setTimetablePeriodId(100L);
        for (AcademicSection section : List.of(sectionA, sectionB)) {
            for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)) {
                FixedActivityPeriod row = new FixedActivityPeriod();
                row.setFixedActivity(activity);
                row.setAcademicGroup(group);
                row.setSection(section);
                row.setDayOfWeek(day);
                row.setTimetablePeriod(period);
                activity.getPeriods().add(row);
            }
        }
        assertEquals(4, activity.getPeriods().size());

        FixedActivityPeriod duplicate = new FixedActivityPeriod();
        duplicate.setFixedActivity(activity);
        duplicate.setAcademicGroup(group);
        duplicate.setSection(sectionA);
        duplicate.setDayOfWeek(DayOfWeek.MONDAY);
        duplicate.setTimetablePeriod(period);
        activity.getPeriods().add(duplicate);
        assertEquals(4, activity.getPeriods().size());
    }
}
