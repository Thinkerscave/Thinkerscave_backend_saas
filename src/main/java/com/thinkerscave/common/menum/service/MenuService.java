package com.thinkerscave.common.menum.service;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;

import java.util.List;
import java.util.Optional;

public interface MenuService {
	Menu saveOrUpdateMenu(MenuDTO dto);

	List<Menu> displayMenudata();

	Optional<Menu> displaySingleMenudata(String code);

	String toggleMenuStatus(String code, boolean status);

}
