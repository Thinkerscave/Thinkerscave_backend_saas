package com.thinkerscave.common.menum.service.impl;

import com.thinkerscave.common.menum.domain.Submenu;
import com.thinkerscave.common.menum.repository.SubmenuRepository;
import com.thinkerscave.common.menum.service.SubmenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubmenuServiceImpl implements SubmenuService {

    @Autowired
    private SubmenuRepository submenuRepo;

    @Override
    public Submenu createSubmenu(Submenu submenu) {
        return submenuRepo.save(submenu);
    }

    @Override
    public Submenu updateSubmenu(Long id, Submenu updatedData) {
        Submenu existing = submenuRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Submenu not found with id: " + id));

        existing.setSubmenuName(updatedData.getSubmenuName());
        existing.setSubmenuCode(updatedData.getSubmenuCode());
        existing.setUrl(updatedData.getUrl());
        existing.setIcon(updatedData.getIcon());
        existing.setSequence(updatedData.getSequence());
        existing.setIsActive(updatedData.getIsActive());
        existing.setTooltip(updatedData.getTooltip());
        existing.setComponentName(updatedData.getComponentName());
        existing.setPermissionKey(updatedData.getPermissionKey());
        existing.setIsVisible(updatedData.getIsVisible());
        existing.setDeleted(updatedData.getDeleted());
        existing.setMenu(updatedData.getMenu());

        return submenuRepo.save(existing);
    }

    @Override
    public Optional<Submenu> getSubmenuById(Long id) {
        return submenuRepo.findById(id);
    }

    @Override
    public List<Submenu> getAllSubmenus() {
        return submenuRepo.findAll();
    }

    @Override
    public List<Submenu> getAllActiveSubmenus() {
        return submenuRepo.findByIsActiveTrue();
    }

    @Override
    public String softDeleteSubmenu(Long id) {
        Optional<Submenu> submenuOptional = submenuRepo.findById(id);
        if (submenuOptional.isPresent()) {
            Submenu submenu = submenuOptional.get();
            submenu.setIsActive(false);
            submenu.setDeleted(true);
            submenuRepo.save(submenu);
            return "Submenu soft deleted successfully.";
        } else {
            throw new RuntimeException("Submenu not found with id: " + id);
        }
    }
}
