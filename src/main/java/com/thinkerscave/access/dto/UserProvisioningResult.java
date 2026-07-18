package com.thinkerscave.access.dto;

import com.thinkerscave.access.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProvisioningResult {

    private final User user;
    private final String temporaryPassword;
}