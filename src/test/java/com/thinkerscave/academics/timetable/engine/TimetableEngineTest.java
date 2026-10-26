package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimetableEngineTest {

    @Test
    void referenceDateIsClampedToAcademicYear() {
        AcademicYear year = new AcademicYear();
        year.setStartDate(LocalDate.of(2026, 4, 1));
        year.setEndDate(LocalDate.of(2027, 3, 31));

        assertThat(TimetablePlanningDataLoader.resolveReferenceDate(year, LocalDate.of(2026, 1, 1)))
                .isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(TimetablePlanningDataLoader.resolveReferenceDate(year, LocalDate.of(2027, 6, 1)))
                .isEqualTo(LocalDate.of(2027, 3, 31));
        assertThat(TimetablePlanningDataLoader.resolveReferenceDate(year, LocalDate.of(2026, 9, 1)))
                .isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void candidatesComeOnlyFromTheGroupsAssignedSlots() {
        TimetablePeriod morning = period(1, "Morning", LocalTime.of(7, 0), LocalTime.of(8, 0));
        TimetablePeriod evening = period(2, "Evening", LocalTime.of(18, 0), LocalTime.of(19, 0));

        SchedulingRequirement morningReq = requirement(10L, DayOfWeek.MONDAY);
        SchedulingRequirement eveningReq = requirement(11L, DayOfWeek.MONDAY);
        SchedulingSourceData source = SchedulingSourceData.builder()
                .workingDays(List.of(DayOfWeek.MONDAY))
                .teachingPeriods(List.of(morning, evening))
                .allPeriods(List.of(morning, evening))
                .schedulesByUnit(Map.of(
                        10L, Map.of(DayOfWeek.MONDAY, List.of(morning)),
                        11L, Map.of(DayOfWeek.MONDAY, List.of(evening))
                ))
                .maxTeacherWeeklyPeriods(30)
                .halfIndex(1)
                .build();

        new CandidateCalculator().calculateCandidates(List.of(morningReq, eveningReq), source);

        assertThat(morningReq.getCandidates()).hasSize(1);
        assertThat(morningReq.getCandidates().get(0).getPeriodId()).isEqualTo(1L);
        assertThat(eveningReq.getCandidates()).hasSize(1);
        assertThat(eveningReq.getCandidates().get(0).getPeriodId()).isEqualTo(2L);
    }

    @Test
    void teacherCanTeachTwoShiftsWhenTimesDoNotOverlap() {
        SchedulingSolution solution = new SchedulingSolution();
        solution.place(placement(10L, 1L, LocalTime.of(7, 0), LocalTime.of(8, 0)));

        assertThat(solution.isTeacherOccupied(1L, DayOfWeek.MONDAY,
                LocalTime.of(18, 0), LocalTime.of(19, 0))).isFalse();
        assertThat(solution.isTeacherOccupied(1L, DayOfWeek.MONDAY,
                LocalTime.of(7, 30), LocalTime.of(8, 30))).isTrue();
    }

    private static TimetablePeriod period(long id, String name, LocalTime start, LocalTime end) {
        TimetablePeriod period = new TimetablePeriod();
        period.setTimetablePeriodId(id);
        period.setPeriodNumber((short) id);
        period.setName(name);
        period.setStartTime(start);
        period.setEndTime(end);
        period.setSlotKind(TimetableSlotKind.TEACHING);
        return period;
    }

    private static SchedulingRequirement requirement(long unitId, DayOfWeek day) {
        SchedulingRequirement requirement = new SchedulingRequirement();
        requirement.setRequirementId("REQ-" + unitId);
        requirement.setUnitId(unitId);
        requirement.setPrimaryStaffId(1L);
        requirement.setWeeklyPeriods(1);
        return requirement;
    }

    private static Placement placement(long unitId, long periodId, LocalTime start, LocalTime end) {
        Placement placement = new Placement();
        placement.setRequirementId("REQ-" + unitId);
        placement.setUnitId(unitId);
        placement.setSectionId(unitId);
        placement.setStaffId(1L);
        placement.setDayOfWeek(DayOfWeek.MONDAY);
        placement.setPeriodId(periodId);
        placement.setPeriodNumber((short) periodId);
        placement.setStartTime(start);
        placement.setEndTime(end);
        placement.setEntryType(TimetableEntryType.SUBJECT);
        return placement;
    }
}
