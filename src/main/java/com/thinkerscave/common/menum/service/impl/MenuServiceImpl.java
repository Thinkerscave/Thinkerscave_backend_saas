package com.thinkerscave.common.menum.service.impl;


import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.repository.MenuRepo;
import com.thinkerscave.common.menum.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
@Service
public class MenuServiceImpl implements MenuService {

	@Autowired
	private MenuRepo menuRepo;

	@Override
	public Menu InsertMenu(MenuDTO menuDTO) {
		try {
			Menu addMenu = new Menu();
			addMenu.setName(menuDTO.getName());
			addMenu.setDescription(menuDTO.getDescription());
			addMenu.setUrl(menuDTO.getUrl());
			addMenu.setIcon(menuDTO.getIcon());
			addMenu.setOrder(menuDTO.getOrder());
			addMenu.setIsActive(menuDTO.getIsActive());
			return menuRepo.save(addMenu);
		} catch (Exception e) {
			throw new RuntimeException("Failed to insert menu: " + e.getMessage());
		}
	}

	@Override
	public List<Menu> displayMenudata() {
		try {
			return menuRepo.findAll();
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu data: " + e.getMessage());
		}
	}

	@Override
	public Menu updateMenudata(Long id, MenuDTO updateMenuData) {
		try {
			Menu existingMenu = menuRepo.findById(id)
					.orElseThrow(() -> new RuntimeException("Menu not found with id: " + id));

			existingMenu.setName(updateMenuData.getName());
			existingMenu.setDescription(updateMenuData.getDescription());
			existingMenu.setUrl(updateMenuData.getUrl());
			existingMenu.setIcon(updateMenuData.getIcon());
			existingMenu.setOrder(updateMenuData.getOrder());
			existingMenu.setIsActive(updateMenuData.getIsActive());

			return menuRepo.save(existingMenu);

		} catch (Exception e) {
			throw new RuntimeException("Failed to update menu: " + e.getMessage());
		}
	}

	@Override
	public Optional<Menu> displaySingleMenudata(Long id) {
		try {
			return menuRepo.findById(id);
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch menu with id " + id + ": " + e.getMessage());
		}
	}

	@Override
	public String softDeleteMenu(Long id) {
		try {
			Optional<Menu> menuOptional = menuRepo.findById(id);

			if (menuOptional.isPresent()) {
				Menu menu = menuOptional.get();
				menu.setIsActive(false); // Mark as inactive
				menuRepo.save(menu); // Save updated status
				return "Menu soft-deleted successfully";
			} else {
				return "Menu not found";
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to soft delete menu: " + e.getMessage());
		}
	}
}
