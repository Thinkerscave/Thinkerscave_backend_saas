package com.thinkerscave.access.dto;

public record UserCreationContext(
        String firstName, String middleName, String lastName,
        String email, String mobileNumber,
        String address, String state, String city,
        String defaultLastName) {
}
