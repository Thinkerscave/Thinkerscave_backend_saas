package com.thinkerscave.common.menum.service.impl;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.repository.MenuRepo;
import com.thinkerscave.common.menum.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for managing Menu entities.
 * Handles creation, update, retrieval, and soft deletion of menu records.
 *
 * @author Sandeep
 */
@Service
public class MenuServiceImpl implements MenuService {

	@Autowired
	private MenuRepo menuRepo;

	/** Saves a new menu or updates an existing one based on the code. */
	@Override
	public Menu saveOrUpdateMenu(String code, MenuDTO dto) {
		Menu menu = null;
		try {
			if (code == null || code.isBlank()) {
				// Create new menu
				menu = new Menu();
				menu.setMenuCode(generateMenuCode(dto.getName()));
			} else {
				// Update existing menu
				menu = menuRepo.findByMenuCode(code)
						.orElseThrow(() -> new RuntimeException("Menu not found with code: " + code));
			}

			// Set fields for both create & update
			menu.setName(dto.getName());
			menu.setDescription(dto.getDescription());
			menu.setUrl(dto.getUrl());
			menu.setIcon(dto.getIcon());
			menu.setOrder(dto.getOrder());
			menu.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

			return menuRepo.save(menu);

		} catch (Exception e) {
			throw new RuntimeException("Failed to save or update menu: " + e.getMessage());
		}
	}

	/** Returns all menu records. */
	@Override
	public List<Menu> displayMenudata() {
		try {
			return menuRepo.findAll();
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu data: " + e.getMessage());
		}
	}

	/** Returns a single menu by its code. */
	@Override
	public Optional<Menu> displaySingleMenudata(String code) {
		try {
			return menuRepo.findByMenuCode(code);
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu with code " + code + ": " + e.getMessage());
		}
	}

	/** Marks a menu as inactive (soft delete) by code. */
	@Override
	public String softDeleteMenu(String code) {
		try {
			Optional<Menu> menuOptional = menuRepo.findByMenuCode(code);

			if (menuOptional.isPresent()) {
				Menu menu = menuOptional.get();
				menu.setIsActive(false);
				menuRepo.save(menu);
				return "Menu soft-deleted successfully";
			} else {
				return "Menu not found";
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to soft delete menu: " + e.getMessage());
		}
	}

	/** Generates a unique menu code from the menu name. */
	private String generateMenuCode(String name) {
		String base = name != null ? name.toUpperCase().replaceAll("\\s+", "_") : "MENU";
		return "MENU_" + base + "_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
	}
}
