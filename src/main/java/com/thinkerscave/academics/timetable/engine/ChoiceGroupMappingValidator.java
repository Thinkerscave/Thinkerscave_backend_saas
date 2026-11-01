package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.SubjectChoiceGroupMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ChoiceGroupMappingValidator {

    private ChoiceGroupMappingValidator() {
    }

    /**
     * Classifies academic-group subjects that belong to an active choice group but are mapped
     * outside of it.
     *
     * <p>A whole group mapped individually is a deliberate setup: every learner takes all of the
     * subjects, so they are scheduled as ordinary lessons and only deserve a warning. A group
     * that is adopted <em>partially</em> (some alternatives through the choice mapping, others
     * mapped on their own) is inconsistent and must block generation, because the two kinds of
     * alternatives would be scheduled with different rules.
     */
    record StandaloneChoiceGroupResult(List<String> blocking, List<String> warnings) {

        boolean hasBlocking() {
            return !blocking.isEmpty();
        }
    }

    static StandaloneChoiceGroupResult findStandaloneMembers(
            Collection<AcademicGroupSubjectMapping> mappings,
            Collection<SubjectChoiceGroupMember> activeMembers) {
        if (mappings == null || activeMembers == null || activeMembers.isEmpty()) {
            return new StandaloneChoiceGroupResult(List.of(), List.of());
        }

        Map<Long, SubjectChoiceGroupMember> memberBySubjectId = new LinkedHashMap<>();
        for (SubjectChoiceGroupMember member : activeMembers) {
            memberBySubjectId.putIfAbsent(member.getSubject().getSubjectId(), member);
        }

        // (academic group | choice group) -> every active mapping of a member subject.
        Map<String, List<AcademicGroupSubjectMapping>> byGroupAndChoice = new LinkedHashMap<>();
        for (AcademicGroupSubjectMapping mapping : mappings) {
            if (!mapping.isActive()) continue;
            SubjectChoiceGroupMember member = memberBySubjectId.get(mapping.getSubject().getSubjectId());
            if (member == null) continue;
            String key = mapping.getAcademicGroup().getAcademicGroupId() + "|"
                    + member.getChoiceGroup().getSubjectChoiceGroupId();
            byGroupAndChoice.computeIfAbsent(key, ignored -> new ArrayList<>()).add(mapping);
        }

        List<String> blocking = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (List<AcademicGroupSubjectMapping> members : byGroupAndChoice.values()) {
            List<AcademicGroupSubjectMapping> viaChoice = new ArrayList<>();
            List<AcademicGroupSubjectMapping> standalone = new ArrayList<>();
            for (AcademicGroupSubjectMapping mapping : members) {
                if (usesActiveChoiceMapping(mapping)) {
                    viaChoice.add(mapping);
                } else {
                    standalone.add(mapping);
                }
            }
            if (standalone.isEmpty()) continue;

            SubjectChoiceGroupMember member = memberBySubjectId.get(
                    standalone.get(0).getSubject().getSubjectId());
            String choiceGroupName = member.getChoiceGroup().getName();
            String groupName = standalone.get(0).getAcademicGroup().getName();

            if (viaChoice.isEmpty()) {
                // The group maps every alternative on its own: all subjects are compulsory
                // lessons for this academic group, so generation can proceed.
                for (AcademicGroupSubjectMapping mapping : standalone) {
                    warnings.add(groupName + " / " + mapping.getSubject().getName()
                            + " is mapped individually and the academic group does not use choice group '"
                            + choiceGroupName + "'. It will be scheduled as a separate lesson.");
                }
            } else {
                for (AcademicGroupSubjectMapping mapping : standalone) {
                    blocking.add(groupName + " / " + mapping.getSubject().getName()
                            + " is mapped individually, but other subjects in choice group '"
                            + choiceGroupName + "' use the choice mapping. Map every alternative "
                            + "through the same choice group.");
                }
            }
        }
        return new StandaloneChoiceGroupResult(List.copyOf(blocking), List.copyOf(warnings));
    }

    private static boolean usesActiveChoiceMapping(AcademicGroupSubjectMapping mapping) {
        AcademicGroupChoiceMapping choiceMapping = mapping.getAcademicGroupChoiceMapping();
        return choiceMapping != null && choiceMapping.isActive();
    }
}
