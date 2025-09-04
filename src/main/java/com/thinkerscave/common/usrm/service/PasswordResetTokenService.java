package com.thinkerscave.common.usrm.service;

import java.util.Optional;

import com.thinkerscave.common.usrm.domain.PasswordResetToken;
import com.thinkerscave.common.usrm.domain.User;

public interface PasswordResetTokenService {

//	PasswordResetToken createToken(User user);

//	Optional<PasswordResetToken> validateToken(String token);
	/**
	 * Creates and sends a 6-digit OTP for the given user.
	 * @param user The user requesting the OTP.
	 * @return The created PasswordResetToken entity.
	 */
	PasswordResetToken createAndSendOtp(User user);

	/**
	 * Validates the provided OTP for a given user.
	 * @param user The user associated with the OTP.
	 * @param otp The 6-digit OTP string to validate.
	 * @return An Optional containing the valid token.
	 */
	Optional<PasswordResetToken> validateOtp(User user, String otp);

}
