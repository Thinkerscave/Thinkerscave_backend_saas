package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import com.thinkerscave.academics.entity.SubjectChoiceGroupMember;
import com.thinkerscave.academics.timetable.engine.ChoiceGroupMappingValidator.StandaloneChoiceGroupResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChoiceGroupMappingValidatorTest {

    @Test
    void reportsActiveChoiceGroupMembersMappedAsStandaloneSubjects() {
        StandaloneChoiceGroupResult result = ChoiceGroupMappingValidator.findStandaloneMembers(
                List.of(mapping(null)), List.of(member()));

        assertTrue(result.blocking().isEmpty());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("Hindi"));
        assertTrue(result.warnings().get(0).contains("Second Language"));
    }

    @Test
    void acceptsSubjectMappedThroughAnActiveChoiceGroup() {
        AcademicGroupChoiceMapping choiceMapping = new AcademicGroupChoiceMapping();
        choiceMapping.setActive(true);

        StandaloneChoiceGroupResult result = ChoiceGroupMappingValidator.findStandaloneMembers(
                List.of(mapping(choiceMapping)), List.of(member()));

        assertTrue(result.blocking().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void reportsSubjectWhoseAcademicChoiceMappingIsInactive() {
        AcademicGroupChoiceMapping choiceMapping = new AcademicGroupChoiceMapping();
        choiceMapping.setActive(false);

        StandaloneChoiceGroupResult result = ChoiceGroupMappingValidator.findStandaloneMembers(
                List.of(mapping(choiceMapping)), List.of(member()));

        assertTrue(result.blocking().isEmpty());
        assertEquals(1, result.warnings().size());
    }

    private static AcademicGroupSubjectMapping mapping(AcademicGroupChoiceMapping choiceMapping) {
        AcademicGroup group = new AcademicGroup();
        group.setAcademicGroupId(7L);
        group.setName("Class 7");
        Subject subject = new Subject();
        subject.setSubjectId(12L);
        subject.setName("Hindi");
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setAcademicGroup(group);
        mapping.setSubject(subject);
        mapping.setActive(true);
        mapping.setAcademicGroupChoiceMapping(choiceMapping);
        return mapping;
    }

    private static SubjectChoiceGroupMember member() {
        Subject subject = new Subject();
        subject.setSubjectId(12L);
        SubjectChoiceGroup choiceGroup = new SubjectChoiceGroup();
        choiceGroup.setSubjectChoiceGroupId(1L);
        choiceGroup.setName("Second Language");
        SubjectChoiceGroupMember member = new SubjectChoiceGroupMember();
        member.setSubject(subject);
        member.setChoiceGroup(choiceGroup);
        return member;
    }
}
