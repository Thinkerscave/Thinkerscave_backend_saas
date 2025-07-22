package com.thinkerscave.common.role.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO {

	private String roleName;
	private String roleCode;
	private String description;
	private Boolean isActive;


}
