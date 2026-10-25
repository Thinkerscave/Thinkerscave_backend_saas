package com.thinkerscave.academics.service.impl;

import com.thinkerscave.shared.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubjectInputRulesTest {

    @Test
    void cleanTextStripsMarkupAndCollapsesWhitespace() {
        assertEquals("Maths 101", SubjectInputRules.cleanText("  <b>Maths</b>\t  101 "));
        assertNull(SubjectInputRules.cleanText("   "));
    }

    @Test
    void requireNameRejectsBlankAndOverlongValues() {
        assertThrows(BusinessException.class, () -> SubjectInputRules.requireName(" <br> "));
        assertThrows(BusinessException.class, () -> SubjectInputRules.requireName("x".repeat(151)));
    }

    @Test
    void normalizeCodeUppercasesAndRejectsUnsafeCharacters() {
        assertEquals("M-1", SubjectInputRules.normalizeCode(" m-1 "));
        assertThrows(BusinessException.class, () -> SubjectInputRules.normalizeCode("M@TH"));
        assertThrows(BusinessException.class, () -> SubjectInputRules.normalizeCode("<script>"));
    }

    @Test
    void cleanDescriptionEnforcesThreeHundredCharacters() {
        assertEquals("ok", SubjectInputRules.cleanDescription(" ok "));
        assertThrows(BusinessException.class, () -> SubjectInputRules.cleanDescription("d".repeat(301)));
    }

    @Test
    void containsPatternEscapesLikeWildcards() {
        assertNull(SubjectInputRules.containsPattern("   "));
        assertEquals("%50!%!_x!!%", SubjectInputRules.containsPattern("50%_x!"));
    }
}
