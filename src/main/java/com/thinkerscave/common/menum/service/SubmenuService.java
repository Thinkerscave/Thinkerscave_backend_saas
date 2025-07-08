package com.thinkerscave.common.menum.service;

import com.thinkerscave.common.menum.domain.Submenu;
import java.util.List;
import java.util.Optional;

public interface SubmenuService {
    Submenu createSubmenu(Submenu submenu);
    Submenu updateSubmenu(Long id, Submenu submenu);
    Optional<Submenu> getSubmenuById(Long id);
    List<Submenu> getAllSubmenus();
    List<Submenu> getAllActiveSubmenus();
    String softDeleteSubmenu(Long id);
}
