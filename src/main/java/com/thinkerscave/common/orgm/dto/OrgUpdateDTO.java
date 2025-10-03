package com.thinkerscave.common.orgm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// Using a record for a concise, immutable data carrier for the request
public record OrgUpdateDTO(
//        @NotNull(message = "isGroup flag cannot be null")
        Boolean isGroup,

        @NotEmpty(message = "Organization name is required")
        String orgName,

        String brandName,
        String orgUrl,
        String city,
        String state,
        String orgType, // Corresponds to 'type' in the 'organisation' table
        LocalDate establishmentDate,

        // Owner Details from the form
        @NotEmpty(message = "Owner name is required")
        String ownerName,

        @NotEmpty(message = "Owner email is required")
        @Email(message = "Please provide a valid email address")
        String ownerEmail,

        String ownerMobile
) {}
