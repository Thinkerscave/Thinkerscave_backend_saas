package com.thinkerscave.common.role.service;


import com.thinkerscave.common.role.DTO.RoleDTO;
import com.thinkerscave.common.role.domain.Role;

import java.util.List;

public interface RoleService {

	public Role saveData(RoleDTO dto);
	public List<Role> allRecords();
	public void delete(Long id);
	public Role editRoleData(Long id);
	public String updateRole( Long id, RoleDTO dto);
}
