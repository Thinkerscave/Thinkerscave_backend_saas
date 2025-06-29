package com.thinkerscave.common.usrm.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

public interface JwtService {

	String generateToken(String userName);
	String generateToken(String userName, int userId);
	String extractUsername(String token);
	Integer extractUserId(String token);
	Date extractExpiration(String token);
	boolean validateToken(String token, UserDetails userDetails);

}
