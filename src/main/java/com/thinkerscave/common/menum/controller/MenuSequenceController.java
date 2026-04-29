package com.thinkerscave.common.menum.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.menum.dto.MenuOrderDTO;
import com.thinkerscave.common.menum.service.MenuService;
import com.thinkerscave.common.commonModel.ApiResponse;

import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/menu-sequence")
@Tag(name = "Menu Sequence", description = "Manage the order of menus and submenus")
@RequiredArgsConstructor
public class MenuSequenceController {

    private final MenuService menuService;

    // Fetch menus with submenus (ordered)
    @io.swagger.v3.oas.annotations.Operation(summary = "Get menu sequence")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuOrderDTO>>> getMenuSequence() {
        log.info("API Request - Get Menu Sequence");
        List<MenuOrderDTO> sequence = menuService.getMenuSequence();
        return ResponseEntity.ok(ApiResponse.success("Menu sequence retrieved", sequence));
    }

    // Save new order
    @io.swagger.v3.oas.annotations.Operation(summary = "Save menu sequence")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveMenuSequence(@RequestBody List<MenuOrderDTO> menuOrders) {
        log.info("API Request - Save Menu Sequence");
        menuService.saveMenuSequence(menuOrders);
        return ResponseEntity.ok(ApiResponse.success("Menu sequence saved successfully", null));
    }
}
