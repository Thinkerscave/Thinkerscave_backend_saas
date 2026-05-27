package com.thinkerscave.common.exam.dto;

import com.thinkerscave.common.exam.domain.MarksStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarksEntryDTO {
    private Long id;

    @NotNull
    private Long examId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long studentId;

    @NotNull
    private Long enrollmentId;

    private BigDecimal marksObtained;
    private BigDecimal maxMarks;
    private String gradeCode;
    private boolean absent;
    private MarksStatus status;
    private Long enteredByUserId;
    private Long approvedByUserId;
    private String remarks;
}
