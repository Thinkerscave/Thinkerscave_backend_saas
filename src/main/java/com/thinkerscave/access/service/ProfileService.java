package com.thinkerscave.access.service;

import com.thinkerscave.access.dto.request.ChangePasswordRequest;
import com.thinkerscave.access.dto.request.UpdateUserRequest;
import com.thinkerscave.access.dto.response.UserSummaryResponse;

public interface ProfileService {

    UserSummaryResponse getCurrentUser();

    UserSummaryResponse updateCurrentUser(UpdateUserRequest request);

    void changeCurrentUserPassword(ChangePasswordRequest request);
}
