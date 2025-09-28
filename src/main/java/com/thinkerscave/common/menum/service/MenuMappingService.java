package com.thinkerscave.common.menum.service;

import java.util.List;

import com.thinkerscave.common.menum.dto.MenuMappingDTO;
import com.thinkerscave.common.menum.dto.RoleMenuMappingRequest;
import com.thinkerscave.common.menum.dto.SideMenuDTO;

public interface MenuMappingService {

	List<SideMenuDTO> getRoleBasedSideMenu(Long roleId);

	List<MenuMappingDTO> getActiveMenuTree();

	void assignRoleMenuPrivileges(RoleMenuMappingRequest request);

}
