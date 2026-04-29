package com.thinkerscave.common.menum.service;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;
import com.thinkerscave.common.menum.dto.MenuOrderDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MenuService {
	MenuDTO saveOrUpdateMenu(MenuDTO dto);

	List<MenuDTO> displayMenudata();

	Optional<MenuDTO> displaySingleMenudata(String code);

	String toggleMenuStatus(String code, boolean status);

	List<Map<String, Object>> getAllActiveMenus();

	List<MenuOrderDTO> getMenuSequence();

	void saveMenuSequence(List<MenuOrderDTO> menuOrders);

}
