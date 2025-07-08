package com.thinkerscave.common.menum.service;


import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.dto.MenuDTO;

import java.util.List;
import java.util.Optional;

public interface MenuService {
	Menu InsertMenu(MenuDTO menu);
	List<Menu> displayMenudata();
	Menu updateMenudata(Long id, MenuDTO updateMenuData);
	Optional<Menu> displaySingleMenudata(Long id);
	String softDeleteMenu(Long id);



}
