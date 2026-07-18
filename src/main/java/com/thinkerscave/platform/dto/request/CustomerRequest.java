package com.thinkerscave.platform.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 3, max = 150, message = "Customer name must be between 3 and 150 characters")
    private String customerName;

    @NotBlank(message = "Business email is required")
    @Email(message = "Business email is invalid")
    @Size(max = 150)
    private String businessEmail;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 7, max = 30, message = "Mobile number is invalid")
    @Pattern(regexp = "^[+]?[0-9\\s\\-]{7,30}$", message = "Mobile number is invalid")
    private String mobileNumber;

    @Size(max = 30)
    @Pattern(regexp = "^$|^[+]?[0-9\\s\\-]{7,30}$", message = "Alternate mobile number is invalid")
    private String alternateMobileNumber;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    @Valid
    @NotNull(message = "Primary contact is required")
    private CustomerContactPayload primaryContact;

    @Valid
    private CustomerContactPayload secondaryContact;
}
