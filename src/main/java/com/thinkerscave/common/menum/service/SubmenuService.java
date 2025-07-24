package com.thinkerscave.common.menum.service;

import com.thinkerscave.common.menum.domain.Submenu;
import com.thinkerscave.common.menum.dto.SubmenuDTO;

import java.util.List;
import java.util.Optional;

public interface SubmenuService {
    Submenu saveOrUpdateSubmenu(String code, Submenu submenu);

    Optional<Submenu> getSubmenu(String code);

    List<Submenu> getAllSubmenus();

    List<Submenu> getAllActiveSubmenus();

    String softDeleteSubmenu(String code);

    void updateSequences(List<SubmenuDTO> sequenceList);
}
