package com.thinkerscave.common.menum.service.impl;

import com.thinkerscave.common.menum.domain.Submenu;
import com.thinkerscave.common.menum.dto.SubmenuDTO;
import com.thinkerscave.common.menum.repository.SubmenuRepo;
import com.thinkerscave.common.menum.service.SubmenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
/**
 * Service implementation for managing Submenu entities.
 * Handles create, update, retrieve, sequencing, and soft deletion operations.
 *
 * @author Sandeep
 */
@Service
public class SubmenuServiceImpl implements SubmenuService {

    @Autowired
    private SubmenuRepo submenuRepo;

    /** Saves a new submenu or updates an existing one by code. */
    @Override
    public Submenu saveOrUpdateSubmenu(String code, Submenu updatedData) {
        Submenu submenu;

        if (code == null || code.isBlank()) {
            // Create new submenu
            submenu = new Submenu();
            submenu.setSubmenuCode(generateSubmenuCode(updatedData.getSubmenuName()));

            // Auto-assign sequence if not provided
            if (updatedData.getSequence() == null) {
                Integer maxSeq = submenuRepo.findMaxSequence();
                submenu.setSequence((maxSeq == null) ? 1 : maxSeq + 1);
            } else {
                submenu.setSequence(updatedData.getSequence());
            }

        } else {
            // Update existing submenu
            submenu = submenuRepo.findBySubmenuCode(code)
                    .orElseThrow(() -> new RuntimeException("Submenu not found with code: " + code));
        }

        submenu.setSubmenuName(updatedData.getSubmenuName());
        submenu.setUrl(updatedData.getUrl());
        submenu.setIcon(updatedData.getIcon());
        submenu.setSequence(updatedData.getSequence());
        submenu.setIsActive(updatedData.getIsActive() != null ? updatedData.getIsActive() : true);
        submenu.setMenu(updatedData.getMenu());

        return submenuRepo.save(submenu);
    }

    /** Returns a submenu by its code. */
    @Override
    public Optional<Submenu> getSubmenu(String code) {
        return submenuRepo.findBySubmenuCode(code);
    }

    /** Returns all submenus sorted by sequence. */
    @Override
    public List<Submenu> getAllSubmenus() {
        return submenuRepo.findAllByOrderBySequenceAsc();
    }

    /** Returns all active submenus sorted by sequence. */
    @Override
    public List<Submenu> getAllActiveSubmenus() {
        return submenuRepo.findByIsActiveTrueOrderBySequenceAsc();
    }

    /** Marks a submenu as inactive (soft delete) by code. */
    @Override
    public String softDeleteSubmenu(String code) {
        Submenu submenu = submenuRepo.findBySubmenuCode(code)
                .orElseThrow(() -> new RuntimeException("Submenu not found with code: " + code));

        submenu.setIsActive(false);
        submenuRepo.save(submenu);
        return "Submenu soft-deleted successfully.";
    }

    /** Updates the sequence numbers of multiple submenus. */
    @Override
    public void updateSequences(List<SubmenuDTO> sequenceList) {
        for (SubmenuDTO dto : sequenceList) {
            Submenu submenu = submenuRepo.findBySubmenuCode(dto.getSubmenuCode())
                    .orElseThrow(() -> new RuntimeException("Submenu not found: " + dto.getSubmenuCode()));
            submenu.setSequence(dto.getSequence());
            submenuRepo.save(submenu);
        }
    }

    /** Generates a unique submenu code from the submenu name. */
    private String generateSubmenuCode(String name) {
        String base = (name != null) ? name.toUpperCase().replaceAll("\\s+", "_") : "SUBMENU";
        return "SUB_" + base + "_" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}