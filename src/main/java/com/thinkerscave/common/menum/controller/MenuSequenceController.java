package com.thinkerscave.common.menum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.menum.dto.MenuOrderDTO;
import com.thinkerscave.common.menum.service.MenuService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menu-sequence")
@RequiredArgsConstructor
public class MenuSequenceController {

    private final MenuService menuService;

    // Fetch menus with submenus (ordered)
    @io.swagger.v3.oas.annotations.Operation(summary = "Get menu sequence", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @GetMapping
    public ResponseEntity<List<MenuOrderDTO>> getMenuSequence() {
        return ResponseEntity.ok(menuService.getMenuSequence());
    }

    // Save new order
    @io.swagger.v3.oas.annotations.Operation(summary = "Save menu sequence", parameters = {
            @io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
    })
    @PostMapping
    public ResponseEntity<Void> saveMenuSequence(@RequestBody List<MenuOrderDTO> menuOrders) {
        menuService.saveMenuSequence(menuOrders);
        return ResponseEntity.ok().build();
    }
}
