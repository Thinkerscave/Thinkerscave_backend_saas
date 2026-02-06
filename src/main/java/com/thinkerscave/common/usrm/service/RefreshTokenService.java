package com.thinkerscave.common.usrm.service;

import java.util.Optional;

import com.thinkerscave.common.usrm.domain.RefreshToken;
import com.thinkerscave.common.usrm.domain.User;

public interface RefreshTokenService {

	public RefreshToken createRefreshToken(String username);

	public Optional<RefreshToken> findByToken(String token);

	public User validateAndGetUser(String token);

	public RefreshToken verifyExpiration(RefreshToken token);

	public void deleteByToken(String refreshToken);
}
