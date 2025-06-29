package com.thinkerscave.common.usrm.dto;

import java.util.List;

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

	public UserResponseDTO(Long id, String userCode, String userName, String email, String firstName, String middleName,
			String lastName, String address, String city, String state, Long mobileNumber, Boolean isBlocked,
			Integer maxDeviceAllow, List<String> roles) {
		this.id = id;
		this.userCode = userCode;
		this.userName = userName;
		this.email = email;
		this.firstName = firstName;
		this.middleName = middleName;
		this.lastName = lastName;
		this.address = address;
		this.city = city;
		this.state = state;
		this.mobileNumber = mobileNumber;
		this.isBlocked = isBlocked;
		this.maxDeviceAllow = maxDeviceAllow;
		this.roles = roles;
	}

	// Getters and Setters
}
