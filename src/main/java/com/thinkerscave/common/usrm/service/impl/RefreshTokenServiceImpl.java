package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.RefreshToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.RefreshTokenRepository;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.service.RefreshTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService{
	
	@Autowired
	private RefreshTokenRepository refreshTokenRepository; 
	@Autowired
	private UserRepository userRepository;

	@Transactional
	public RefreshToken createRefreshToken(String username) {
		User user = userRepository.findByUserName(username)
				.orElseThrow(() -> new RuntimeException("User not found with username: " + username));

		// Check if a token already exists for this user
		Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUser((user));

		RefreshToken refreshToken;
		if (existingTokenOpt.isPresent()) {
			// If it exists, UPDATE the existing token
			refreshToken = existingTokenOpt.get();
			refreshToken.setToken(UUID.randomUUID().toString());
			refreshToken.setExpiryDate(Instant.now().plusMillis(600000)); // Update expiry
		} else {
			// If it doesn't exist, CREATE a new one
			refreshToken = RefreshToken.builder()
					.user(user)
					.token(UUID.randomUUID().toString())
					.expiryDate(Instant.now().plusMillis(600000))
					.build();
		}

		// Save the (either updated or new) token
		return refreshTokenRepository.save(refreshToken);
	}
	/**
     * Find a refresh token by its token value.
     *
     * @param token the token value
     * @return an Optional containing the refresh token if found, or empty if not found
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
	
	public RefreshToken verifyExpiration(RefreshToken token) {
		if(token.getExpiryDate().compareTo(Instant.now()) < 0) {
			
			refreshTokenRepository.delete(token);
			throw new RuntimeException(token.getToken()+"Refresh token was expired");
		}
		return token;
	}
	@Override
	public void deleteByToken(String refreshToken) {
		refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);	
	}
}