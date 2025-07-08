package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.domain.Submenu;
import com.thinkerscave.common.menum.service.SubmenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/submenu")
public class SubmenuController {

    @Autowired
    private SubmenuService submenuService;

    // ✅ Create Submenu
    @PostMapping
    public ResponseEntity<Object> createSubmenu(@RequestBody Submenu submenu) {
        Submenu created = submenuService.createSubmenu(submenu);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ✅ Update Submenu
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateSubmenu(@PathVariable Long id, @RequestBody Submenu submenu) {
        try {
            Submenu updated = submenuService.updateSubmenu(id, submenu);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ✅ Get Submenu by ID
    @GetMapping("/{id}")
    public ResponseEntity<Object> getSubmenuById(@PathVariable Long id) {
        Optional<Submenu> submenu = submenuService.getSubmenuById(id);
        if (submenu.isPresent()) {
            return ResponseEntity.ok(submenu.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submenu not found with ID: " + id);
        }
    }

    // ✅ Get All Submenus
    @GetMapping
    public ResponseEntity<Object> getAllSubmenus() {
        List<Submenu> list = submenuService.getAllSubmenus();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(list);
    }

    // ✅ Get All Active Submenus
    @GetMapping("/active")
    public ResponseEntity<Object> getActiveSubmenus() {
        List<Submenu> list = submenuService.getAllActiveSubmenus();
        if (list.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return ResponseEntity.ok(list);
    }

    // ✅ Soft Delete Submenu
    @PutMapping("/delete/{id}")
    public ResponseEntity<Object> softDeleteSubmenu(@PathVariable Long id) {
        try {
            String message = submenuService.softDeleteSubmenu(id);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
