package com.thinkerscave.common.exam.dto;

import com.thinkerscave.common.exam.domain.ResultStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultDTO {
    private Long id;
    private Long examId;
    private Long studentId;
    private Long enrollmentId;
    private BigDecimal totalMarks;
    private BigDecimal maxMarks;
    private BigDecimal percentage;
    private BigDecimal gpa;
    private String gradeCode;
    private Integer classRank;
    private Integer sectionRank;
    private ResultStatus status;
    private String remarks;
}
