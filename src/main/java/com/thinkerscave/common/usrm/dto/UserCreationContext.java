package com.thinkerscave.common.usrm.dto;

public record UserCreationContext(
        String firstName, String middleName, String lastName,
        String email, String mobileNumber,
        String address, String state, String city,
        String defaultLastName) {
}
