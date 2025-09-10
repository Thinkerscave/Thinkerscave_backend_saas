package com.thinkerscave.common.menum.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.menum.dto.SideMenuDTO;
import com.thinkerscave.common.menum.service.MenuMappingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/menu-mapping")
@CrossOrigin("http://localhost:4200/")
@RequiredArgsConstructor
public class MenuMappingController {

    private final MenuMappingService menuMappingService;

    @GetMapping
    public List<SideMenuDTO> getSideMenu() {
        List<SideMenuDTO> menus = menuMappingService.getSideMenu();

        // Add static Dashboard menu at the top
        SideMenuDTO dashboard = new SideMenuDTO("Dashboard", "pi pi-home", "/app", null);
        menus.add(0, dashboard);

        return menus;
    }
}
