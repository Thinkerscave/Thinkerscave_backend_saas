package com.thinkerscave.common.menum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.menum.domain.SubMenu;
import com.thinkerscave.common.menum.domain.SubMenuPrivilegeMapping;

@Repository
public interface SubMenuPrivilegeMappingRepository extends JpaRepository<SubMenuPrivilegeMapping, Long> {
    void deleteBySubMenu(SubMenu subMenu);
}

