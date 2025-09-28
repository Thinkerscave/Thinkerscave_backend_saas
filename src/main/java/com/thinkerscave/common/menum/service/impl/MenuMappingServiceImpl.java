package com.thinkerscave.common.menum.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.Privilege;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;
import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.dto.MenuMappingDTO;
import com.thinkerscave.common.menum.dto.RoleMenuMappingRequest;
import com.thinkerscave.common.menum.dto.SideMenuDTO;
import com.thinkerscave.common.menum.dto.SubMenuMappingDTO;
import com.thinkerscave.common.menum.repository.MenuRepository;
import com.thinkerscave.common.menum.repository.PrivilegeRepository;
import com.thinkerscave.common.menum.repository.RoleMenuPrivilegeMappingRepository;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.menum.repository.SubMenuRepository;
import com.thinkerscave.common.menum.service.MenuMappingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuMappingServiceImpl implements MenuMappingService {

	private final MenuRepository menuRepository;
	private final SubMenuRepository subMenuRepository;
	private final RoleMenuPrivilegeMappingRepository roleMenuPrivilegeMappingRepository;
	private final RoleRepository roleRepository;
	private final PrivilegeRepository privilegeRepository;

	@Override
	public List<SideMenuDTO> getRoleBasedSideMenu(Long roleId) {
		List<RoleMenuPrivilegeMapping> mappings = roleMenuPrivilegeMappingRepository.findByRoleId(roleId);

        // Group by Menu → SubMenu
        Map<Menu, Map<SubMenu, List<Privilege>>> grouped = mappings.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSubMenu().getMenu(),
                        Collectors.groupingBy(
                                RoleMenuPrivilegeMapping::getSubMenu,
                                Collectors.mapping(RoleMenuPrivilegeMapping::getPrivilege, Collectors.toList())
                        )
                ));

        List<SideMenuDTO> menuList = new ArrayList<>();

        for (Map.Entry<Menu, Map<SubMenu, List<Privilege>>> menuEntry : grouped.entrySet()) {
            Menu menu = menuEntry.getKey();

            List<SideMenuDTO> subMenuDTOs = menuEntry.getValue().entrySet().stream()
                    .map(subEntry -> {
                        SubMenu subMenu = subEntry.getKey();
                        List<String> privileges = subEntry.getValue().stream()
                                .map(Privilege::getPrivilegeName) // assuming privilege has name
                                .toList();

                        return new SideMenuDTO(
                                subMenu.getSubMenuName(),
                                subMenu.getSubMenuIcon(),
                                subMenu.getSubMenuUrl(),
                                null,
                                privileges
                        );
                    })
                    .toList();

            SideMenuDTO menuDTO = new SideMenuDTO(
                    menu.getName(),
                    menu.getIcon(),
                    menu.getUrl(),
                    subMenuDTOs,
                    Collections.emptyList() // top-level menus don’t have privileges
            );

            menuList.add(menuDTO);
        }
        // Add static Dashboard at the top
        SideMenuDTO dashboard = new SideMenuDTO("Dashboard", "pi pi-home", "/app", null, Collections.emptyList());
        menuList.add(0, dashboard);
        
        return menuList;
	}

	@Override
	public List<MenuMappingDTO> getActiveMenuTree() {
		List<Menu> menus = menuRepository.findByIsActiveTrueOrderByMenuOrderAsc();
		List<MenuMappingDTO> menuList = new ArrayList<>();

		for (Menu menu : menus) {
			List<SubMenu> subMenus = subMenuRepository.findByMenuAndIsActiveTrueOrderBySubMenuOrderAsc(menu);

			if (subMenus.isEmpty())
				continue; // skip menus without submenus

			List<SubMenuMappingDTO> subMenuDTOs = new ArrayList<>();
			for (SubMenu subMenu : subMenus) {
				List<Privilege> privileges = subMenu.getPrivilegeMappings().stream()
						.map(mapping -> new Privilege(mapping.getPrivilege().getPrivilegeId(),
								mapping.getPrivilege().getPrivilegeName()))
						.toList();

				subMenuDTOs.add(new SubMenuMappingDTO(subMenu.getSubMenuId(), subMenu.getSubMenuName(),
						subMenu.getSubMenuUrl(), privileges));
			}

			menuList.add(new MenuMappingDTO(menu.getMenuId(), menu.getName(), menu.getIcon(), subMenuDTOs));
		}
		return menuList;
	}

	@Override
	public void assignRoleMenuPrivileges(RoleMenuMappingRequest request) {
		Long roleId = request.getRoleId();
		Role role = roleRepository.findById(roleId).get();

        // 1. Delete existing mappings for the role
		roleMenuPrivilegeMappingRepository.deleteByRole(role);

        // 2. Insert new mappings
        for (RoleMenuMappingRequest.SubMenuPrivilegeDTO sm : request.getSubMenuPrivileges()) {
            Long subMenuId = sm.getSubMenuId();
            for (Long privilegeId : sm.getPrivilegeIds()) {
            	RoleMenuPrivilegeMapping roleMenuPrivilegeMapping = new RoleMenuPrivilegeMapping();
            	roleMenuPrivilegeMapping.setRole(role);
            	roleMenuPrivilegeMapping.setSubMenu(subMenuRepository.findById(subMenuId).get());
            	roleMenuPrivilegeMapping.setPrivilege(privilegeRepository.findById(privilegeId).get());
            	roleMenuPrivilegeMappingRepository.save(roleMenuPrivilegeMapping);
            }
        }
		
	}

}
