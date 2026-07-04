package com.thinkerscave.admission.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublicInquiryRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String mobileNumber;

    private String email;

    @NotBlank
    private String classInterestedIn;

    private String address;
    private String inquirySource;
    private String comments;
}