package com.thinkerscave.finance.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PayrollGenerateResult {
    private Long runId;
    private String status;
    private int generatedCount;
    private int skippedCount;
    @Builder.Default
    private List<SkipInfo> skipped = new ArrayList<>();

    @Data
    @Builder
    public static class SkipInfo {
        private Long staffId;
        private String reason;
    }
}
