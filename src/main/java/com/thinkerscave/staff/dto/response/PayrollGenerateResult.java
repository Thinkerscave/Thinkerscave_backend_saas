package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Result of a tenant-scoped payroll generation run.
 */
@Getter
@Builder
public class PayrollGenerateResult {

    private final int generatedRecords;
    private final int skippedAlreadyExists;
    /** Staff codes skipped because no active salary structure was found. */
    @Builder.Default
    private final List<String> skippedNoSalaryStructure = Collections.emptyList();
    /** Tenant identifier under which generation ran (audit / debug). */
    private final String tenantIdentifier;
}
