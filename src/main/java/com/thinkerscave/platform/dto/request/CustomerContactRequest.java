package com.thinkerscave.platform.dto.request;

import com.thinkerscave.platform.enums.ContactType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerContactRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 30)
    @Pattern(regexp = "^[+]?[0-9\\s\\-]{7,30}$", message = "Mobile number is invalid")
    private String mobileNumber;

    @Size(max = 100)
    private String designation;

    @NotNull(message = "Contact type is required")
    private ContactType contactType;
}
