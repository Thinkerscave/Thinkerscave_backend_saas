package com.thinkerscave.common.menum.service.impl;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.dto.SubMenuRequestDTO;
import com.thinkerscave.common.menum.dto.SubMenuResponseDTO;
import com.thinkerscave.common.menum.repository.MenuRepository;
import com.thinkerscave.common.menum.repository.SubMenuRepository;
import com.thinkerscave.common.menum.service.SubMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing Submenu entities. Handles create, update,
 * retrieve, sequencing, and active filtering operations.
 */
@Service
public class SubMenuServiceImpl implements SubMenuService {

	@Autowired
	private SubMenuRepository subMenuRepository;

	@Autowired
	private MenuRepository menuRepository;

	/**
	 * Saves a new submenu or updates an existing one. - Uses submenuId for updates
	 * (DB identity). - Generates submenuCode only for new records.
	 */
	@Override
	public SubMenuResponseDTO saveOrUpdateSubMenu(SubMenuRequestDTO dto) {
		SubMenu subMenu;

		if (dto.getSubMenuId() == null) {
			// ✅ Create new submenu
			subMenu = new SubMenu();
			subMenu.setSubMenuCode(generateSubmenuCode(dto.getSubMenuCode()));
			// Auto-assign sequence if not provided
			if (dto.getSubMenuOrder() == null) {
				Integer maxSeq = subMenuRepository.findMaxSequence();
				subMenu.setSubMenuOrder((maxSeq == null) ? 1 : maxSeq + 1);
			} else {
				subMenu.setSubMenuOrder(dto.getSubMenuOrder());
			}

		} else {
			// ✅ Update existing submenu
			subMenu = subMenuRepository.findById(dto.getSubMenuId())
					.orElseThrow(() -> new RuntimeException("Submenu not found with id: " + dto.getSubMenuId()));
		}

		// ✅ Set fields
		subMenu.setSubMenuName(dto.getSubMenuName());
		subMenu.setSubMenuUrl(dto.getSubMenuUrl());
		subMenu.setSubMenuIcon(dto.getSubMenuIcon());
		subMenu.setSubMenuOrder(dto.getSubMenuOrder());
		subMenu.setIsActive(dto.getSubMenuIsActive() != null ? dto.getSubMenuIsActive() : true);
		subMenu.setSubMenuDescription(dto.getSubMenuDescription());

		// ✅ Fetch Menu by ID
		Menu menu = menuRepository.findById(dto.getMenuId())
				.orElseThrow(() -> new RuntimeException("Menu not found with id: " + dto.getMenuId()));
		subMenu.setMenu(menu);

		subMenu = subMenuRepository.save(subMenu);

		return mapToResponseDTO(subMenu);
	}

	/**
	 * Returns a submenu by its public code (safe for external APIs).
	 */
	@Override
	public Optional<SubMenuResponseDTO> getSubMenuByCode(String code) {
		return subMenuRepository.findBySubMenuCode(code).map(this::mapToResponseDTO);
	}

	/**
	 * Returns all submenus sorted by sequence.
	 */
	@Override
	public List<SubMenuResponseDTO> getAllSubMenus() {
		return subMenuRepository.findAllByOrderBySubMenuOrderAsc().stream().map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Returns all active submenus sorted by sequence.
	 */
	@Override
	public List<SubMenuResponseDTO> getAllActiveSubMenus() {
		return subMenuRepository.findByIsActiveTrueOrderBySubMenuOrderAsc().stream().map(this::mapToResponseDTO)
				.collect(Collectors.toList());
	}

	/**
	 * Utility method to generate a unique submenu code.
	 */
	private String generateSubmenuCode(String name) {
		String base = (name != null) ? name.toUpperCase().replaceAll("\\s+", "_") : "SUBMENU";
		return "SUB_" + base + "_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
	}

	/**
	 * Maps a Submenu entity to SubMenuResponseDTO.
	 */
	private SubMenuResponseDTO mapToResponseDTO(SubMenu subMenu) {
		SubMenuResponseDTO dto = new SubMenuResponseDTO();
		dto.setSubMenuId(subMenu.getSubMenuId());
		dto.setSubMenuName(subMenu.getSubMenuName());
		dto.setSubMenuCode(subMenu.getSubMenuCode());
		dto.setSubMenuUrl(subMenu.getSubMenuUrl());
		dto.setSubMenuIcon(subMenu.getSubMenuIcon());
		dto.setSubMenuOrder(subMenu.getSubMenuOrder());
		dto.setSubMenuIsActive(subMenu.getIsActive());
		dto.setSubMenuDescription(subMenu.getSubMenuDescription());
		dto.setMenuId(subMenu.getMenu().getMenuId());
		dto.setMenuName(subMenu.getMenu().getName());
		dto.setMenuCode(subMenu.getMenu().getMenuCode());
		dto.setCreatedBy(subMenu.getCreatedBy());
		dto.setLastUpdatedOn(subMenu.getLastModifiedDate());
		return dto;
	}

	@Override
	public String updateSubMenuStatus(String code, boolean status) {
		try {
	        Optional<SubMenu> subMenuOptional = subMenuRepository.findBySubMenuCode(code);
	        if (subMenuOptional.isPresent()) {
	            SubMenu subMenu = subMenuOptional.get();
	            subMenu.setIsActive(status);
	            subMenuRepository.save(subMenu);
	            return "Sub-menu status updated to " + (status ? "Active" : "Inactive");
	        } else {
	            return "Sub-Menu not found";
	        }
	    } catch (Exception e) {
	        throw new RuntimeException("Failed to update sub-menu status: " + e.getMessage());
	    }
	}
}
