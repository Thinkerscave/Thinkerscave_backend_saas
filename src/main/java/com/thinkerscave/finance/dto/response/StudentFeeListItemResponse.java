package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StudentFeeListItemResponse {
    private Long studentId;
    private String studentName;
    private String admissionNumber;
    private String className;
    private String sectionName;
    private BigDecimal totalFee;
    private BigDecimal paid;
    private BigDecimal outstanding;
    private BigDecimal advance;
    private boolean canCollectFee;
}
