package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class TransferRequestDTO {
    private Long id;
    private String requestNumber;
    private Long studentId;
    private Long enrollmentId;
    private String reason;
    private String destinationSchool;
    private String status;
    private LocalDate requestedOn;
    private String certificateNumber;
}
