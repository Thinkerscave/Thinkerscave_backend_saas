package com.thinkerscave.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.thinkerscave.common.menum.service.impl.SubMenuServiceImpl;
import com.thinkerscave.common.menum.service.impl.RoleServiceImpl;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DbCheckRunner implements CommandLineRunner {

    private final SubMenuServiceImpl subMenuService;
    private final RoleServiceImpl roleService;

    @Override
    public void run(String... args) {
        System.out.println("\n\n====== REPOSITORY CHECK START ======");

        try {
            System.out.println("Checking subMenuService.getAllSubMenus()...");
            subMenuService.getAllSubMenus();
            System.out.println("SubMenuService PASSED");
        } catch (Exception e) {
            System.out.println("SubMenuService ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            System.out.println("Checking roleService.getAllRoles()...");
            roleService.getAllRoles();
            System.out.println("RoleService PASSED");
        } catch (Exception e) {
            System.out.println("RoleService ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("====== REPOSITORY CHECK END ======\n\n");
    }
}
