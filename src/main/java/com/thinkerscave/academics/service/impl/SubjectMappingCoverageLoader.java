package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.repository.IdCount;
import com.thinkerscave.academics.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Batch counts of active sections covered by group-wide subject mappings. */
@Component
@RequiredArgsConstructor
class SubjectMappingCoverageLoader {

    private final SectionRepository sectionRepository;

    Map<Long, Integer> activeSectionCounts(Collection<Long> groupIds) {
        if (groupIds.isEmpty()) return Map.of();
        return toCounts(sectionRepository.countActiveByGroupIds(groupIds));
    }

    private static Map<Long, Integer> toCounts(Collection<IdCount> rows) {
        Map<Long, Integer> counts = new HashMap<>();
        for (IdCount row : rows) {
            counts.put(row.getId(), Math.toIntExact(row.getTotal()));
        }
        return counts;
    }
}
