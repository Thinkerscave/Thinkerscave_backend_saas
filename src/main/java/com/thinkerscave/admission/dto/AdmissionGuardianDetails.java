package com.thinkerscave.admission.dto;

public record AdmissionGuardianDetails(
        String fullName,
        String relationship,
        String mobile,
        String email,
        String occupation,
        Boolean primaryContact) {
}
