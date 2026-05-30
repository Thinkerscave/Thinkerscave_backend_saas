package com.thinkerscave.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.thinkerscave.common.menum.service.impl.SubMenuServiceImpl;
import com.thinkerscave.common.menum.service.impl.RoleServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbCheckRunner implements CommandLineRunner {

    private final SubMenuServiceImpl subMenuService;
    private final RoleServiceImpl roleService;

    @Override
    public void run(String... args) {
        log.info("====== REPOSITORY CHECK START ======");

        try {
            log.debug("Checking subMenuService.getAllSubMenus()...");
            subMenuService.getAllSubMenus();
            log.info("SubMenuService PASSED");
        } catch (Exception e) {
            log.error("SubMenuService ERROR: {}", e.getMessage(), e);
        }

        try {
            log.debug("Checking roleService.getAllRoles()...");
            roleService.getAllRoles();
            log.info("RoleService PASSED");
        } catch (Exception e) {
            log.error("RoleService ERROR: {}", e.getMessage(), e);
        }

        log.info("====== REPOSITORY CHECK END ======");
    }
}
