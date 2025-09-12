package com.thinkerscave.common.menum.service;

import java.util.List;
import java.util.Optional;

import com.thinkerscave.common.menum.domain.Privilege;
import com.thinkerscave.common.menum.dto.SubMenuRequestDTO;
import com.thinkerscave.common.menum.dto.SubMenuResponseDTO;

public interface SubMenuService {

	SubMenuResponseDTO saveOrUpdateSubMenu(SubMenuRequestDTO subMenuDTO);

	Optional<SubMenuResponseDTO> getSubMenuByCode(String code);

	List<SubMenuResponseDTO> getAllSubMenus();

	List<SubMenuResponseDTO> getAllActiveSubMenus();

	String updateSubMenuStatus(String code, boolean status);

	List<Privilege> getAllPrivileges();
}
