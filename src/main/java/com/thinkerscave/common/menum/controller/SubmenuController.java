package com.thinkerscave.common.menum.controller;

import com.thinkerscave.common.menum.domain.Submenu;
import com.thinkerscave.common.menum.dto.SubmenuDTO;
import com.thinkerscave.common.menum.service.SubmenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/submenu")
public class SubmenuController {

    @Autowired
    private SubmenuService submenuService;

    // ✅ Create Submenu
    @PostMapping
    public ResponseEntity<Object> createSubmenu(@RequestBody Submenu submenu) {
        Submenu created = submenuService.saveOrUpdateSubmenu(null, submenu); // null means create
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ✅ Update Submenu by Code
    @PutMapping("/{code}")
    public ResponseEntity<Object> updateSubmenu(@PathVariable String code, @RequestBody Submenu submenu) {
        try {
            Submenu updated = submenuService.saveOrUpdateSubmenu(code, submenu);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // ✅ Get Submenu by Code
    @GetMapping("/{code}")
    public ResponseEntity<Object> getSubmenuByCode(@PathVariable String code) {
        Optional<Submenu> submenu = submenuService.getSubmenu(code);
        if (submenu.isPresent()) {
            return ResponseEntity.ok(submenu.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Submenu not found with code: " + code);
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
    @PutMapping("/delete/{code}")
    public ResponseEntity<Object> softDeleteSubmenu(@PathVariable String code) {
        try {
            String message = submenuService.softDeleteSubmenu(code);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/sequence-update")
    public ResponseEntity<Object> updateSubmenuSequences(@RequestBody List<SubmenuDTO> sequenceList) {
        try {
            submenuService.updateSequences(sequenceList);
            return ResponseEntity.ok("Submenu sequences updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
