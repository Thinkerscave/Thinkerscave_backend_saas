package com.thinkerscave.finance.service;

public interface FeeGenerationService {
    /** API path — requires FEES_SETTINGS:MANAGE. */
    int generate(Long academicYearId, Long classId);

    /** Scheduled / system path — no interactive privilege check. */
    int generateSystem(Long academicYearId, Long classId);
}
