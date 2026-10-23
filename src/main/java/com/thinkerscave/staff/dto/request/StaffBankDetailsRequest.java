package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffBankDetailsRequest {

    @NotBlank(message = "Bank name is required")
    @Size(max = 150)
    private String bankName;

    @NotBlank(message = "Account holder name is required")
    @Size(max = 150)
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{6,18}$", message = "Account number must be 6 to 18 digits")
    private String accountNumber;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "IFSC code is invalid")
    private String ifscCode;
}
