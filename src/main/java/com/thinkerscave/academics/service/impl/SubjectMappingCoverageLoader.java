package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.enums.SubjectMappingScope;
import com.thinkerscave.academics.repository.AcademicGroupSubjectMappingSectionRepository;
import com.thinkerscave.academics.repository.IdCount;
import com.thinkerscave.academics.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Batch lookups of section coverage so list and mapping views avoid per-row queries. */
@Component
@RequiredArgsConstructor
class SubjectMappingCoverageLoader {

    private final SectionRepository sectionRepository;
    private final AcademicGroupSubjectMappingSectionRepository mappingSectionRepository;

    Map<Long, Integer> activeSectionCounts(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        return toCounts(sectionRepository.countActiveByGroupIds(groupIds));
    }

    Map<Long, Integer> selectedSectionCounts(Collection<Long> mappingIds) {
        if (mappingIds.isEmpty()) return Map.of();
        return toCounts(mappingSectionRepository.countByMappingIds(mappingIds));
    }

    int coveredSectionCount(AcademicGroupSubjectMapping mapping,
                            Map<Long, Integer> activeSectionCounts,
                            Map<Long, Integer> selectedSectionCounts) {
        if (mapping.getScope() == SubjectMappingScope.SELECTED_SECTIONS) {
            return selectedSectionCounts.getOrDefault(mapping.getAcademicGroupSubjectMappingId(), 0);
        }
        return activeSectionCounts.getOrDefault(mapping.getAcademicGroup().getAcademicGroupId(), 0);
    }

    static List<AcademicSection> sortedByDisplayOrder(List<AcademicSection> sections) {
        return sections.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() == null ? 0 : a.getDisplayOrder(),
                        b.getDisplayOrder() == null ? 0 : b.getDisplayOrder()))
                .toList();
    }

    private static Map<Long, Integer> toCounts(List<IdCount> rows) {
        Map<Long, Integer> counts = new HashMap<>();
        for (IdCount row : rows) {
            counts.put(row.getId(), Math.toIntExact(row.getTotal()));
        }
        return counts;
    }
}
