package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.SubjectMappingScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicGroupSubjectMappingScopeTest {

    @Test
    void allSectionsScopeAppliesToEverySection() {
        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();

        assertTrue(mapping.appliesToSection(11L));
    }

    @Test
    void selectedSectionsScopeAppliesOnlyToConfiguredSections() {
        AcademicSection included = new AcademicSection();
        included.setSectionId(11L);
        AcademicGroupSubjectMappingSection coverage = new AcademicGroupSubjectMappingSection();
        coverage.setSection(included);

        AcademicGroupSubjectMapping mapping = new AcademicGroupSubjectMapping();
        mapping.setScope(SubjectMappingScope.SELECTED_SECTIONS);
        mapping.setSectionMappings(List.of(coverage));

        assertTrue(mapping.appliesToSection(11L));
        assertFalse(mapping.appliesToSection(12L));
        assertFalse(mapping.appliesToSection(null));
    }
}