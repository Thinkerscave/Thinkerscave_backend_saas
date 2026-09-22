package com.thinkerscave.finance.dto.response;

import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FeeStructureResponse {
    private Long feeStructureId;
    private String name;
    private Long academicYearId;
    private String academicYearName;
    private AcademicYearStatus academicYearStatus;
    private boolean yearEditable;
    private Long classId;
    private String className;
    private String classCode;
    private Short dueDay;
    private FeeMasterStatus status;
    /** Annualized mandatory total (monthly×12, etc.). */
    private BigDecimal mandatorySum;
    /** Annualized optional total. */
    private BigDecimal optionalSum;
    /** Annualized mandatory + optional. */
    private BigDecimal totalFees;
    /** Sum of monthly-frequency item amounts (not annualized), or null if mixed. */
    private BigDecimal configuredMonthlyAmount;
    private Integer feeHeadCount;
    private LocalDateTime updatedOn;
    private String updatedBy;
    private List<FeeStructureItemResponse> items;
}
