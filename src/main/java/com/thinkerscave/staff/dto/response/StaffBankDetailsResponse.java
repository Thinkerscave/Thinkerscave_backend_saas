package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Masked bank details. The account number is never returned; only the last 4 digits.
 * Payroll prefill reads bank name, holder name, IFSC and the masked number from this view.
 */
@Getter
@Setter
@Builder
public class StaffBankDetailsResponse {

    private Long staffId;
    private String bankName;
    private String accountHolderName;
    private String maskedAccountNumber;
    private String ifscCode;
    private LocalDateTime updatedOn;
}
