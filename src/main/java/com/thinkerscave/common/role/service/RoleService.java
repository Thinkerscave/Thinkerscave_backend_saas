package com.thinkerscave.common.role.service;


import com.thinkerscave.common.role.dto.RoleDTO;
import com.thinkerscave.common.role.domain.Role;

import java.util.List;

public interface RoleService {

	String saveOrUpdateRole(String code, RoleDTO dto);
	List<Role> allRecords();
	void delete(String code);
	Role editRoleData(String code);

}
