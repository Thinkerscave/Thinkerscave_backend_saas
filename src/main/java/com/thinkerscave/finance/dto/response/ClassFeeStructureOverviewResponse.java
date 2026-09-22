package com.thinkerscave.finance.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row in the class-wise fee structure overview (Configured / Not Configured).
 */
@Data
@Builder
public class ClassFeeStructureOverviewResponse {
    private Long classId;
    private String className;
    private String classCode;
    private boolean configured;
    private Long feeStructureId;
    private BigDecimal requiredFees;
    private BigDecimal optionalFees;
    private BigDecimal totalFees;
    private BigDecimal configuredMonthlyAmount;
    private Integer feeHeadCount;
    private Short dueDay;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearEditable;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
