package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TeacherArrangementResponse {
    private Long arrangementId;
    private Long slotId;
    private Long absentTeacherId;
    private Long substituteTeacherId;
    private LocalDate arrangementDate;
    private String status;
    private String reason;
    private Long approvedBy;
    private LocalDateTime createdOn;
}
