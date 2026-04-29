package com.thinkerscave.common.usrm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthRequest {

	@com.fasterxml.jackson.annotation.JsonProperty("userName")
	private String username;
	private String password;

}
