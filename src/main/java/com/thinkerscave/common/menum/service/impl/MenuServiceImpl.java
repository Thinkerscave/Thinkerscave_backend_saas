package com.thinkerscave.common.menum.service.impl;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.dto.MenuOrderDTO;
import com.thinkerscave.common.menum.dto.SubMenuOrderDTO;
import com.thinkerscave.common.menum.repository.MenuRepository;
import com.thinkerscave.common.menum.repository.SubMenuRepository;
import com.thinkerscave.common.menum.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Menu entities. Handles creation, update,
 * retrieval, and soft deletion of menu records.
 *
 * @author Sandeep
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

	private final MenuRepository menuRepository;
	private final SubMenuRepository subMenuRepository;

	/** Saves a new menu or updates an existing one based on the code. */
	@Override
	public Menu saveOrUpdateMenu(MenuDTO dto) {
		Menu menu = null;
		try {
			if (dto.getMenuCode() == null || dto.getMenuCode().isBlank()) {
				// Create new menu
				menu = new Menu();
				menu.setMenuCode(generateMenuCode(dto.getName()));
			} else {
				// Update existing menu
				menu = menuRepository.findByMenuCode(dto.getMenuCode())
						.orElseThrow(() -> new RuntimeException("Menu not found with code: " + dto.getMenuCode()));
			}

			// Set fields for both create & update
			menu.setName(dto.getName());
			menu.setDescription(dto.getDescription());
			menu.setUrl(dto.getUrl());
			menu.setIcon(dto.getIcon());
			menu.setMenuOrder(dto.getOrder());
			menu.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

			return menuRepository.save(menu);

		} catch (Exception e) {
			throw new RuntimeException("Failed to save or update menu: " + e.getMessage());
		}
	}

	/** Returns all menu records. */
	@Override
	public List<Menu> displayMenudata() {
		try {
			return menuRepository.findAll();
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu data: " + e.getMessage());
		}
	}

	@Override
	public List<Map<String, Object>> getAllActiveMenus() {
		return menuRepository.findByIsActiveTrueOrderByMenuOrderAsc().stream()
				.map(menu -> Map.<String, Object>of("menuId",
						menu.getMenuId(), "name", menu.getName(), "menuCode", menu.getMenuCode()))
				.toList();
	}

	@Override
	public String toggleMenuStatus(String code, boolean status) {
		try {
			Optional<Menu> menuOptional = menuRepository.findByMenuCode(code);
			if (menuOptional.isPresent()) {
				Menu menu = menuOptional.get();
				menu.setIsActive(status);
				menuRepository.save(menu);
				return "Menu status updated to " + (status ? "Active" : "Inactive");
			} else {
				return "Menu not found";
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to update menu status: " + e.getMessage());
		}
	}

	/** Generates a unique menu code from the menu name. */
	private String generateMenuCode(String name) {
		String base = name != null ? name.toUpperCase().replaceAll("\\s+", "_") : "MENU";
		return "MENU_" + base + "_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
	}

	/** Returns a single menu by its code. */
	@Override
	public Optional<Menu> displaySingleMenudata(String code) {
		try {
			return menuRepository.findByMenuCode(code);
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu with code " + code + ": " + e.getMessage());
		}
	}

	@Override
	public List<MenuOrderDTO> getMenuSequence() {
		// Fetch all menus ordered
		List<Menu> menus = menuRepository.findAllByOrderByMenuOrderAsc();

		// Fetch all submenus in one go
		List<SubMenu> allSubMenus = subMenuRepository.findAllByOrderBySubMenuOrderAsc();

		// Group submenus by menuId (not by Menu object to avoid mismatches)
		Map<Long, List<SubMenu>> subMenusByMenu = allSubMenus.stream()
				.collect(Collectors.groupingBy(sub -> sub.getMenu().getMenuId()));

		// Map menus to DTOs
		return menus.stream().map(menu -> {
			List<SubMenuOrderDTO> subMenuDTOs = subMenusByMenu.getOrDefault(menu.getMenuId(), List.of()).stream()
					.map(sub -> new SubMenuOrderDTO(sub.getSubMenuId(), sub.getSubMenuName(), sub.getSubMenuCode(),
							sub.getSubMenuOrder()))
					.toList();

			return new MenuOrderDTO(menu.getMenuId(), menu.getName(), menu.getMenuCode(), menu.getMenuOrder(),
					subMenuDTOs);
		}).toList();
	}

	@Transactional
	public void saveMenuSequence(List<MenuOrderDTO> menuOrders) {
		List<Menu> menus = new ArrayList<>();
		List<SubMenu> subMenus = new ArrayList<>();

		for (MenuOrderDTO menuOrder : menuOrders) {
			Menu menu = new Menu();
			menu.setMenuId(menuOrder.getMenuId());
			menu.setMenuOrder(menuOrder.getMenuOrder());
			menus.add(menu);

			for (SubMenuOrderDTO sub : menuOrder.getSubMenus()) {
				SubMenu submenu = new SubMenu();
				submenu.setSubMenuId(sub.getSubMenuId());
				submenu.setSubMenuOrder(sub.getSubMenuOrder());
				subMenus.add(submenu);
			}
		}

		menuRepository.saveAll(menus);
		subMenuRepository.saveAll(subMenus);
	}

}
