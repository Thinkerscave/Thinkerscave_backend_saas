package com.thinkerscave.common.usrm.service;

import java.util.Optional;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;

public interface PasswordResetTokenService {

	PasswordResetToken createToken(User user);

	Optional<PasswordResetToken> validateToken(String token);

}
