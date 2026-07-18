package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.dto.UserProvisioningResult;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;

public interface UserService {
    User createUser(UserCreationContext context, Role role);

    UserProvisioningResult createUserWithTemporaryPassword(UserCreationContext context, Role role);
}
