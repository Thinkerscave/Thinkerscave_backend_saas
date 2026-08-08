package com.thinkerscave.communication.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ParticipantCsv exact-token matching")
class ParticipantCsvTest {

    @Nested
    @DisplayName("contains")
    class Contains {

        @Test
        @DisplayName("user 1 does not match CSV containing 21")
        void userOneDoesNotMatchTwentyOne() {
            assertFalse(ParticipantCsv.contains("21", 1L));
            assertFalse(ParticipantCsv.contains("21,100", 1L));
            assertFalse(ParticipantCsv.contains("100,21", 1L));
        }

        @Test
        @DisplayName("user 1 matches exact token 1")
        void userOneMatchesExact() {
            assertTrue(ParticipantCsv.contains("1", 1L));
            assertTrue(ParticipantCsv.contains("1,21", 1L));
            assertTrue(ParticipantCsv.contains("21,1,100", 1L));
            assertTrue(ParticipantCsv.contains("100,1", 1L));
        }

        @Test
        @DisplayName("user 21 matches only exact 21")
        void userTwentyOneExact() {
            assertTrue(ParticipantCsv.contains("21", 21L));
            assertTrue(ParticipantCsv.contains("1,21,100", 21L));
            assertFalse(ParticipantCsv.contains("1,210,100", 21L));
            assertFalse(ParticipantCsv.contains("2,1", 21L));
        }

        @Test
        @DisplayName("null/blank csv or null userId returns false")
        void nullSafe() {
            assertFalse(ParticipantCsv.contains(null, 1L));
            assertFalse(ParticipantCsv.contains("", 1L));
            assertFalse(ParticipantCsv.contains("  ", 1L));
            assertFalse(ParticipantCsv.contains("1,2", null));
        }
    }

    @Nested
    @DisplayName("join / parse / ensureContains")
    class JoinParse {

        @Test
        void joinDeduplicates() {
            assertEquals("1,21", ParticipantCsv.join(List.of(1L, 21L, 1L)));
        }

        @Test
        void parseRoundTrip() {
            Set<Long> ids = ParticipantCsv.parse("1,21,100");
            assertEquals(Set.of(1L, 21L, 100L), ids);
        }

        @Test
        void ensureContainsAddsCurrentUser() {
            assertEquals("21,1", ParticipantCsv.ensureContains("21", 1L));
            assertEquals("1,21", ParticipantCsv.ensureContains("1,21", 1L));
        }
    }
}
