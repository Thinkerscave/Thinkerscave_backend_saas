package com.thinkerscave.common.usrm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponse {

	private String accessToken;
	private String token; // refresh token

	// Multi-tenant authentication enhancement
	private String tenantId;
	private String tenantName;

	// User information
	private UserResponseDTO user;

}
