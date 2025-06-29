package com.thinkerscave.common.usrm.service;

public interface LoginAttemptService {
    void loginSucceeded(String username);
    void loginFailed(String username);
    boolean isBlocked(String username);
}

