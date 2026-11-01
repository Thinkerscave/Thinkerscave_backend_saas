package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessCheck;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessSeverity;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import com.thinkerscave.academics.entity.SubjectChoiceGroupMember;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimetableReadinessEvaluatorTest {

    @Test
    void warnsButDoesNotBlockWhenTheWholeChoiceGroupIsMappedIndividually() {
        AcademicGroup group = group(7L, "Class 7");
        SubjectChoiceGroup choiceGroup = choiceGroup(1L, "Second Language");
        Subject hindi = subject(12L, "Hindi");

        AcademicGroupSubjectMapping mapping = mapping(group, hindi, null);

        List<ReadinessCheck> checks = evaluate(
                List.of(mapping), List.of(member(choiceGroup, hindi)));

        ReadinessCheck warning = check(checks, "CHOICE_GROUP_MAPPED_INDIVIDUALLY");
        assertEquals(ReadinessSeverity.WARNING, warning.getSeverity());
        assertTrue(warning.getDetails().get(0).contains("Hindi"));
        assertTrue(warning.getDetails().get(0).contains("Second Language"));
        assertTrue(checks.stream()
                .noneMatch(c -> "CHOICE_GROUP_MEMBERS_MAPPED_INDIVIDUALLY".equals(c.getCode())));
    }

    @Test
    void blocksWhenAChoiceGroupIsOnlyPartiallyAdopted() {
        AcademicGroup group = group(8L, "Class 8");
        SubjectChoiceGroup choiceGroup = choiceGroup(1L, "Second Language");
        Subject hindi = subject(12L, "Hindi");
        Subject sanskrit = subject(13L, "Sanskrit");
        AcademicGroupChoiceMapping choiceMapping = new AcademicGroupChoiceMapping();
        choiceMapping.setAcademicGroupChoiceMappingId(5L);
        choiceMapping.setAcademicGroup(group);
        choiceMapping.setChoiceGroup(choiceGroup);
        choiceMapping.setActive(true);

        AcademicGroupSubjectMapping hindiStandalone = mapping(group, hindi, null);
        AcademicGroupSubjectMapping sanskritViaChoice = mapping(group, sanskrit, choiceMapping);

        List<ReadinessCheck> checks = evaluate(
                List.of(hindiStandalone, sanskritViaChoice),
                List.of(member(choiceGroup, hindi), member(choiceGroup, sanskrit)));

        ReadinessCheck blocker = check(checks, "CHOICE_GROUP_MEMBERS_MAPPED_INDIVIDUALLY");
        assertEquals(ReadinessSeverity.BLOCKING, blocker.getSeverity());
        assertTrue(blocker.getDetails().get(0).contains("Hindi"));
        assertTrue(blocker.getDetails().get(0).contains("Second Language"));
    }

    private static List<ReadinessCheck> evaluate(
            List<AcademicGroupSubjectMapping> mappings,
            List<SubjectChoiceGroupMember> members) {
        TimetablePlanningData data = TimetablePlanningData.builder()
                .mappings(mappings)
                .activeChoiceGroupMembers(members)
                .build();
        List<ReadinessCheck> checks = new ArrayList<>();
        new TimetableReadinessEvaluator(null, null).checkChoiceGroups(data, checks);
        return checks;
    }

    private static ReadinessCheck check(List<ReadinessCheck> checks, String code) {
        return checks.stream()
                .filter(check -> code.equals(check.getCode()))
                .findFirst()
                .orElseThrow();
    }

    private static AcademicGroup group(long id, String name) {
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(id);
        group.setName(name);
        return group;
    }

    private static Subject subject(long id, String name) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        return subject;
    }

    private static SubjectChoiceGroup choiceGroup(long id, String name) {
        SubjectChoiceGroup choiceGroup = new SubjectChoiceGroup();
        choiceGroup.setSubjectChoiceGroupId(id);
        choiceGroup.setName(name);
        choiceGroup.setActive(true);
        return choiceGroup;
    }

    private static SubjectChoiceGroupMember member(SubjectChoiceGroup choiceGroup, Subject subject) {
        SubjectChoiceGroupMember member = new SubjectChoiceGroupMember();
        member.setSubject(subject);
        member.setChoiceGroup(choiceGroup);
        return member;
    }

    private static AcademicGroupSubjectMapping mapping(AcademicGroup group, Subject subject,
                                                      AcademicGroupChoiceMapping choiceMapping) {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroup(group);
        mapping.setSubject(subject);
        mapping.setAcademicGroupChoiceMapping(choiceMapping);
        mapping.setActive(true);
        return mapping;
    }
}
