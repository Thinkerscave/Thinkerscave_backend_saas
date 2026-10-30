package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
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

    @Test
    void choiceAlternativesShareOnlyTheirMatchingGroupPeriodSlot() {
        SchedulingSolution solution = new SchedulingSolution();
        Placement hindi = placement(10L, 1L, LocalTime.of(7, 0), LocalTime.of(8, 0));
        hindi.setAcademicGroupSubjectMappingId(101L);
        hindi.setChoiceMappingId(7L);
        hindi.setPeriodIndex(1);
        hindi.setSubjectId(201L);
        solution.place(hindi);

        SchedulingRequirement sanskrit = requirement(10L, DayOfWeek.MONDAY);
        sanskrit.setChoiceMappingId(7L);
        sanskrit.setPeriodIndex(1);
        sanskrit.setAcademicGroupSubjectMappingId(102L);
        sanskrit.setSubjectId(202L);
        assertThat(solution.isSectionOccupiedFor(sanskrit, DayOfWeek.MONDAY,
                LocalTime.of(7, 0), LocalTime.of(8, 0))).isFalse();

        SchedulingRequirement unrelatedSubject = requirement(10L, DayOfWeek.MONDAY);
        assertThat(solution.isSectionOccupiedFor(unrelatedSubject, DayOfWeek.MONDAY,
                LocalTime.of(7, 0), LocalTime.of(8, 0))).isTrue();

        SchedulingRequirement differentChoicePeriod = requirement(10L, DayOfWeek.MONDAY);
        differentChoicePeriod.setChoiceMappingId(7L);
        differentChoicePeriod.setPeriodIndex(2);
        differentChoicePeriod.setAcademicGroupSubjectMappingId(102L);
        differentChoicePeriod.setSubjectId(202L);
        assertThat(solution.isSectionOccupiedFor(differentChoicePeriod, DayOfWeek.MONDAY,
                LocalTime.of(7, 0), LocalTime.of(8, 0))).isTrue();
    }

    @Test
    void choiceAlternativesCannotBePlacedInDifferentWeekdaySlots() {
        SchedulingSolution solution = new SchedulingSolution();
        Placement hindi = placement(10L, 1L, LocalTime.of(7, 0), LocalTime.of(8, 0));
        hindi.setChoiceMappingId(7L);
        hindi.setPeriodIndex(0);
        solution.place(hindi);

        SchedulingRequirement sanskrit = requirement(10L, DayOfWeek.TUESDAY);
        sanskrit.setChoiceMappingId(7L);
        sanskrit.setPeriodIndex(0);

        assertThat(solution.choicePartnerAtDifferentSlot(sanskrit, DayOfWeek.TUESDAY, 1L)).isTrue();
        assertThat(solution.choicePartnerAtDifferentSlot(sanskrit, DayOfWeek.MONDAY, 1L)).isFalse();
    }

    @Test
    void choiceAlternativesAreScheduledInTheSameSlot() {
        TimetablePeriod p1 = period(1, "P1", LocalTime.of(7, 0), LocalTime.of(8, 0));
        TimetablePeriod p2 = period(2, "P2", LocalTime.of(8, 0), LocalTime.of(9, 0));
        SchedulingSourceData source = SchedulingSourceData.builder()
                .workingDays(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
                .teachingPeriods(List.of(p1, p2))
                .allPeriods(List.of(p1, p2))
                .schedulesByUnit(Map.of(10L, Map.of(
                        DayOfWeek.MONDAY, List.of(p1, p2),
                        DayOfWeek.TUESDAY, List.of(p1, p2))))
                .maxTeacherWeeklyPeriods(30)
                .halfIndex(1)
                .build();

        SchedulingRequirement hindi = choiceRequirement("REQ-HINDI", 2L, 101L, 201L);
        SchedulingRequirement sanskrit = choiceRequirement("REQ-SANSKRIT", 3L, 102L, 202L);
        new CandidateCalculator().calculateCandidates(List.of(hindi, sanskrit), source);

        SchedulingSolution solution = new ConstraintScheduler(new CandidateScorer())
                .solve(List.of(hindi, sanskrit), source, 11L, new GenerationProgress(1L));

        assertThat(solution.getUnplacedRequirements()).isEmpty();
        List<Placement> placed = new java.util.ArrayList<>(solution.getPlacements());
        assertThat(placed).hasSize(2);
        assertThat(placed.get(0).getDayOfWeek()).isEqualTo(placed.get(1).getDayOfWeek());
        assertThat(placed.get(0).getPeriodId()).isEqualTo(placed.get(1).getPeriodId());
    }

    @Test
    void wholeSchoolFixedActivityDoesNotCreateTeacherClash() {
        Placement first = placement(10L, 1L, LocalTime.of(7, 0), LocalTime.of(8, 0));
        first.setStaffId(5L);
        first.setFixedActivityId(99L);
        first.setEntryType(TimetableEntryType.ACTIVITY);
        Placement second = placement(11L, 1L, LocalTime.of(7, 0), LocalTime.of(8, 0));
        second.setStaffId(5L);
        second.setFixedActivityId(99L);
        second.setEntryType(TimetableEntryType.ACTIVITY);

        SchedulingSolution solution = new SchedulingSolution();
        solution.place(first);
        solution.place(second);

        TimetablePeriod p1 = period(1, "P1", LocalTime.of(7, 0), LocalTime.of(8, 0));
        SchedulingSourceData source = SchedulingSourceData.builder()
                .workingDays(List.of(DayOfWeek.MONDAY))
                .teachingPeriods(List.of(p1))
                .allPeriods(List.of(p1))
                .schedulesByUnit(Map.of())
                .maxTeacherWeeklyPeriods(30)
                .halfIndex(1)
                .build();

        SolutionValidator.ValidationResult result = new SolutionValidator()
                .validate(solution, List.of(), source);

        assertThat(result.getBlockingConflicts())
                .noneMatch(c -> c.getConflictType()
                        == com.thinkerscave.academics.enums.TimetableConflictType.TEACHER_CONFLICT);
    }

    @Test
    void computesFirstHalfPerDayPattern() {
        com.thinkerscave.academics.entity.SchedulePattern weekday =
                new com.thinkerscave.academics.entity.SchedulePattern();
        weekday.setSchedulePatternId(3L);
        com.thinkerscave.academics.entity.SchedulePattern saturday =
                new com.thinkerscave.academics.entity.SchedulePattern();
        saturday.setSchedulePatternId(4L);

        TimetablePeriod w1 = period(1, "P1", LocalTime.of(10, 30), LocalTime.of(11, 15), weekday);
        TimetablePeriod w2 = period(2, "P2", LocalTime.of(11, 15), LocalTime.of(12, 0), weekday);
        TimetablePeriod w3 = period(3, "P3", LocalTime.of(12, 0), LocalTime.of(12, 45), weekday);
        TimetablePeriod w4 = period(4, "P4", LocalTime.of(12, 45), LocalTime.of(13, 30), weekday);
        TimetablePeriod wBreak = breakPeriod(5, LocalTime.of(13, 30), LocalTime.of(14, 10), weekday);
        TimetablePeriod w5 = period(6, "P5", LocalTime.of(14, 10), LocalTime.of(14, 50), weekday);
        TimetablePeriod w6 = period(7, "P6", LocalTime.of(14, 50), LocalTime.of(15, 30), weekday);

        TimetablePeriod s1 = period(11, "P1", LocalTime.of(7, 0), LocalTime.of(7, 40), saturday);
        TimetablePeriod s2 = period(12, "P2", LocalTime.of(7, 40), LocalTime.of(8, 20), saturday);
        TimetablePeriod s3 = period(13, "P3", LocalTime.of(8, 20), LocalTime.of(9, 0), saturday);
        TimetablePeriod sBreak = breakPeriod(14, LocalTime.of(9, 0), LocalTime.of(9, 30), saturday);
        TimetablePeriod s4 = period(15, "P4", LocalTime.of(9, 30), LocalTime.of(10, 5), saturday);
        TimetablePeriod s5 = period(16, "P5", LocalTime.of(10, 5), LocalTime.of(10, 40), saturday);

        List<TimetablePeriod> weekdayPeriods = List.of(w1, w2, w3, w4, wBreak, w5, w6);
        List<TimetablePeriod> saturdayPeriods = List.of(s1, s2, s3, sBreak, s4, s5);
        TimetablePlanningData data = TimetablePlanningData.builder()
                .workingDays(List.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY))
                .scheduleByUnit(Map.of(10L, Map.of(
                        DayOfWeek.MONDAY, weekdayPeriods,
                        DayOfWeek.SATURDAY, saturdayPeriods)))
                .allPeriods(List.of(w1, w2, w3, w4, wBreak, w5, w6,
                        s1, s2, s3, sBreak, s4, s5))
                .build();

        Map<DayOfWeek, Integer> halfIndex = TimetableGenerationEngine.halfIndexByDay(data);

        assertThat(halfIndex.get(DayOfWeek.MONDAY)).isEqualTo(4);
        assertThat(halfIndex.get(DayOfWeek.SATURDAY)).isEqualTo(3);
    }

    private static TimetablePeriod period(long id, String name, LocalTime start, LocalTime end,
                                          com.thinkerscave.academics.entity.SchedulePattern pattern) {
        TimetablePeriod period = period(id, name, start, end);
        period.setSchedulePattern(pattern);
        return period;
    }

    private static TimetablePeriod breakPeriod(long id, LocalTime start, LocalTime end,
                                               com.thinkerscave.academics.entity.SchedulePattern pattern) {
        TimetablePeriod period = period(id, "Break", start, end);
        period.setSlotKind(TimetableSlotKind.BREAK);
        period.setSchedulePattern(pattern);
        return period;
    }

    @Test
    void optimizerSwapsLessonsIntoTheirPreferredHalf() {
        TimetablePeriod p1 = period(1, "P1", LocalTime.of(8, 0), LocalTime.of(9, 0));
        TimetablePeriod p2 = period(2, "P2", LocalTime.of(9, 0), LocalTime.of(10, 0));
        TimetablePeriod p3 = period(3, "P3", LocalTime.of(10, 0), LocalTime.of(11, 0));
        TimetablePeriod p4 = period(4, "P4", LocalTime.of(11, 0), LocalTime.of(12, 0));
        SchedulingSourceData source = SchedulingSourceData.builder()
                .workingDays(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
                .teachingPeriods(List.of(p1, p2, p3, p4))
                .allPeriods(List.of(p1, p2, p3, p4))
                .schedulesByUnit(Map.of(10L, Map.of(
                        DayOfWeek.MONDAY, List.of(p1, p2, p3, p4),
                        DayOfWeek.TUESDAY, List.of(p1, p2, p3, p4))))
                .maxTeacherWeeklyPeriods(30)
                .halfIndex(2)
                .build();

        SchedulingRequirement english = requirement(10L, DayOfWeek.MONDAY);
        english.setRequirementId("REQ-ENGLISH");
        english.setAcademicGroupSubjectMappingId(101L);
        english.setSubjectId(1L);
        english.setSubjectName("English");
        english.setPrimaryStaffId(11L);
        english.setWeeklyPeriods(1);
        english.setPeriodIndex(0);
        english.setPreference(SubjectTimetablePreference.FIRST_HALF);

        SchedulingRequirement history = requirement(10L, DayOfWeek.MONDAY);
        history.setRequirementId("REQ-HISTORY");
        history.setAcademicGroupSubjectMappingId(102L);
        history.setSubjectId(2L);
        history.setSubjectName("History");
        history.setPrimaryStaffId(5L);
        history.setWeeklyPeriods(1);
        history.setPeriodIndex(0);
        history.setPreference(SubjectTimetablePreference.SECOND_HALF);

        SchedulingSolution solution = new SchedulingSolution();
        solution.place(new Placement(english, slot(DayOfWeek.MONDAY, p3, 2)));
        solution.place(new Placement(history, slot(DayOfWeek.TUESDAY, p1, 0)));

        new SoftConstraintOptimizer().optimize(solution, source, new GenerationProgress(1L),
                Map.of("REQ-ENGLISH", english, "REQ-HISTORY", history));

        Placement englishAfter = placementFor(solution, "REQ-ENGLISH");
        Placement historyAfter = placementFor(solution, "REQ-HISTORY");
        assertThat(englishAfter.getTeachingIndex()).isLessThan(2);
        assertThat(historyAfter.getTeachingIndex()).isGreaterThanOrEqualTo(2);
    }

    private static Placement placementFor(SchedulingSolution solution, String requirementId) {
        return solution.getPlacements().stream()
                .filter(p -> requirementId.equals(p.getRequirementId()))
                .findFirst()
                .orElseThrow();
    }

    private static SchedulingRequirement choiceRequirement(String id, long staffId,
                                                           long mappingId, long subjectId) {
        SchedulingRequirement requirement = requirement(10L, DayOfWeek.MONDAY);
        requirement.setRequirementId(id);
        requirement.setPrimaryStaffId(staffId);
        requirement.setChoiceMappingId(7L);
        requirement.setPeriodIndex(0);
        requirement.setAcademicGroupSubjectMappingId(mappingId);
        requirement.setSubjectId(subjectId);
        requirement.setSubjectName(id);
        return requirement;
    }

    private static CandidateSlot slot(DayOfWeek day, TimetablePeriod period, int teachingIndex) {
        return new CandidateSlot(day, 10L, 10L, 1L, period.getTimetablePeriodId(),
                period.getPeriodNumber(), period.getStartTime(), period.getEndTime(),
                teachingIndex, null, null);
    }

    private static TimetablePeriod period(long id, String name, LocalTime start, LocalTime end) {        TimetablePeriod period = new TimetablePeriod();
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
