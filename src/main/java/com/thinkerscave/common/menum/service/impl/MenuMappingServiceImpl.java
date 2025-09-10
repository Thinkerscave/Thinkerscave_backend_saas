package com.thinkerscave.common.menum.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.dto.SideMenuDTO;
import com.thinkerscave.common.menum.repository.MenuRepository;
import com.thinkerscave.common.menum.repository.SubMenuRepository;
import com.thinkerscave.common.menum.service.MenuMappingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuMappingServiceImpl implements MenuMappingService {

	private final MenuRepository menuRepository;
	private final SubMenuRepository subMenuRepository;

	@Override
	public List<SideMenuDTO> getSideMenu() {
		// Fetch active menus
		List<Menu> menus = menuRepository.findByIsActiveTrueOrderByMenuOrderAsc();

		List<SideMenuDTO> menuList = new ArrayList<>();

		for (Menu menu : menus) {
			// Fetch active submenus for each menu
			List<SubMenu> subMenus = subMenuRepository.findByMenuAndIsActiveTrueOrderBySubMenuOrderAsc(menu);

			// Map submenus to DTO
			List<SideMenuDTO> subMenuDTOs = subMenus.stream()
					.map(sub -> new SideMenuDTO(sub.getSubMenuName(), sub.getSubMenuIcon(), sub.getSubMenuUrl(), null // no
																														// deeper
																														// nesting
																														// for
																														// now
					)).toList();

			// Map menu
			SideMenuDTO menuDTO = new SideMenuDTO(menu.getName(), menu.getIcon(), menu.getUrl(),
					subMenuDTOs.isEmpty() ? null : subMenuDTOs);

			menuList.add(menuDTO);
		}

		return menuList;
	}

}
