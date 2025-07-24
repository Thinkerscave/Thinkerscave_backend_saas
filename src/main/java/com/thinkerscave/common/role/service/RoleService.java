package com.thinkerscave.common.role.service;


import com.thinkerscave.common.role.dto.RoleDTO;
import com.thinkerscave.common.role.domain.Role;

import java.util.List;

public interface RoleService {

	/** Saves or updates a role based on the given code and data. */
	String saveOrUpdateRole(String code, RoleDTO dto);

	/** Returns all role records. */
	List<Role> allRecords();

	/** Deletes a role by its code. */
	void delete(String code);

	/** Returns role data by code for editing. */
	Role editRoleData(String code);
}
