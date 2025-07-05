package com.thinkerscave.common.usrm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
	private Long id;
	private String userCode;
	private String userName;
	private String email;
	private String firstName;
	private String middleName;
	private String lastName;
	private String address;
	private String city;
	private String state;
	private Long mobileNumber;
	private Boolean isBlocked;
	private Integer maxDeviceAllow;
	private List<String> roles;


}
