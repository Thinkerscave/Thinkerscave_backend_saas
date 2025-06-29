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
	    // Find the user by username
	    Optional<User> optionalUser = userRepository.findByUserName(username);

	    // If user not found, throw an exception
	    if (!optionalUser.isPresent()) {
	        throw new RuntimeException("User not found with username: " + username);
	    }

	    // Extract the User object from the Optional
	    User user = optionalUser.get();

	     // Remove existing refresh tokens for the user
	      //refreshTokenRepository.deleteByUser(user);

	        // Generate a new refresh token
	        RefreshToken refreshToken = RefreshToken.builder()
	                .user(user)
	                .token(UUID.randomUUID().toString())
	                .expiryDate(Instant.now().plusMillis(600000))
	                .build();

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
}