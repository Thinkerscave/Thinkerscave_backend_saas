package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.fee.domain.ReminderChannel;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeReminderDTO {

    private Long id;
    private Long feeInvoiceId;
    private Long studentId;
    private ReminderChannel channel;
    private String recipient;
    private String subject;
    private Instant sentAt;
    private boolean success;
    private String failureReason;
    private Integer attemptNumber;
}
