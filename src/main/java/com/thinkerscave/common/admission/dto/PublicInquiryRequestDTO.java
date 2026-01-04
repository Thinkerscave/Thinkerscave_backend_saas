package com.thinkerscave.common.admission.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicInquiryRequestDTO {

    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String mobileNumber;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String classInterestedIn;

    @NotBlank
    @Size(min = 10, max = 500)
    private String address;
}

