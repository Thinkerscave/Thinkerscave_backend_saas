package com.thinkerscave.staff.service;

import com.thinkerscave.shared.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaffCodeGeneratorTest {

    @Test
    void usesFullShortNameWhenAtMostEightCharacters() {
        assertEquals("ALPHA", StaffCodeGenerator.deriveOrgCode("Alpha"));
    }

    @Test
    void stripsNonAlphanumericAndTruncatesLongNames() {
        assertEquals("AKAS", StaffCodeGenerator.deriveOrgCode("Akashic Institute Bhubaneswar"));
        assertEquals("DABP", StaffCodeGenerator.deriveOrgCode("dab-public school"));
    }

    @Test
    void rejectsMissingOrTooShortShortName() {
        assertThrows(BadRequestException.class, () -> StaffCodeGenerator.deriveOrgCode(null));
        assertThrows(BadRequestException.class, () -> StaffCodeGenerator.deriveOrgCode(" "));
        assertThrows(BadRequestException.class, () -> StaffCodeGenerator.deriveOrgCode("A-"));
    }
}
