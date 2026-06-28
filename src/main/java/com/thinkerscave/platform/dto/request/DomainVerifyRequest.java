package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomainVerifyRequest {

    @NotBlank
    @Size(max = 255)
    private String domain;

    @Size(max = 255)
    private String verificationToken;
}
