package com.thinkerscave.common.promotion.dto;

import com.thinkerscave.common.promotion.domain.TransferStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {
    private Long id;
    private String requestNumber;
    private Long studentId;
    private Long enrollmentId;
    private LocalDate requestedOn;
    private String reason;
    private String destinationSchool;
    private TransferStatus status;
    private Long approvedByUserId;
    private LocalDate approvedOn;
    private String certificateNumber;
    private LocalDate certificateIssuedOn;
    private String remarks;
}
