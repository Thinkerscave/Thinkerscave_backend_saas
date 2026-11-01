package com.thinkerscave.academics.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherCoverageServiceImplTest {

    @Test
    void ranksPrimarySubjectMatchFirstThenSecondaryThenOthers() {
        assertEquals(1, TeacherCoverageServiceImpl.preferenceRank(10L, 20L, 10L));
        assertEquals(2, TeacherCoverageServiceImpl.preferenceRank(10L, 20L, 20L));
        assertEquals(3, TeacherCoverageServiceImpl.preferenceRank(10L, 20L, 30L));
        assertEquals(3, TeacherCoverageServiceImpl.preferenceRank(null, null, 30L));
        assertEquals(3, TeacherCoverageServiceImpl.preferenceRank(10L, 20L, null));
    }

    @Test
    void classifiesWorkloadAgainstCapacity() {
        assertEquals("AVAILABLE", TeacherCoverageServiceImpl.workloadStatus(10, 24));
        assertEquals("LIMITED", TeacherCoverageServiceImpl.workloadStatus(21, 24));
        assertEquals("AT_CAPACITY", TeacherCoverageServiceImpl.workloadStatus(24, 24));
        assertEquals("EXCEEDS_LIMIT", TeacherCoverageServiceImpl.workloadStatus(25, 24));
    }

    @Test
    void keysAreStableAcrossSectionAndMapping() {
        assertEquals("7:42", TeacherCoverageServiceImpl.key(7L, 42L));
        assertTrue(!TeacherCoverageServiceImpl.key(7L, 42L).equals(TeacherCoverageServiceImpl.key(42L, 7L)));
    }
}
