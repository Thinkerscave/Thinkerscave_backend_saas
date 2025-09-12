package com.thinkerscave.common.menum.service;

import java.util.List;
import com.thinkerscave.common.menum.dto.RoleDTO;
import com.thinkerscave.common.menum.dto.RoleLookupDTO;

public interface RoleService {

	RoleDTO saveOrUpdateRole(RoleDTO dto);

	List<RoleDTO> getAllRoles();

	RoleDTO getRoleByCode(String roleCode);

	void updateRoleStatus(Long roleId, Boolean status);

	List<RoleLookupDTO> getActiveRoles();

}
