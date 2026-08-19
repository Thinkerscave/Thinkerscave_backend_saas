package com.thinkerscave.access.repository;

import com.thinkerscave.access.entity.Menu;
import com.thinkerscave.access.enums.MenuScope;
import com.thinkerscave.access.enums.MenuType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {

    Optional<Menu> findByMenuCode(String menuCode);

    boolean existsByMenuCode(String menuCode);

    boolean existsByMenuCodeAndIdNot(String menuCode, Long id);

    List<Menu> findByActiveTrueAndParentMenuIsNullOrderByDisplayOrderAsc();

    List<Menu> findByActiveTrueOrderByDisplayOrderAsc();

    List<Menu> findByMenuTypeAndActiveTrueOrderByDisplayOrderAsc(MenuType menuType);

    @Query("SELECT m FROM Menu m WHERE m.active = true AND m.showInSidebar = true AND m.parentMenu IS NULL ORDER BY m.displayOrder ASC")
    List<Menu> findTopLevelSidebarMenus();

    @Query("SELECT m FROM Menu m WHERE m.active = true AND m.showInSidebar = true AND m.parentMenu.id = :parentId ORDER BY m.displayOrder ASC")
    List<Menu> findSidebarChildMenus(@Param("parentId") Long parentId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Menu m WHERE m.parentMenu.id = :menuId")
    boolean hasChildren(@Param("menuId") Long menuId);

    List<Menu> findByParentMenu_Id(Long parentId);

    List<Menu> findByParentMenu_IdAndActiveTrue(Long parentId);

    List<Menu> findByMenuScopeAndParentMenuIsNullAndActiveTrue(MenuScope menuScope);

    List<Menu> findByFeature_IdInAndParentMenuIsNullAndActiveTrue(List<Long> featureIds);

    List<Menu> findByFeature_Id(Long featureId);

    List<Menu> findAllByOrderByDisplayOrderAsc();

    List<Menu> findByParentMenuIsNullOrderByDisplayOrderAsc();

    List<Menu> findByMenuCodeInAndActiveTrue(List<String> menuCodes);

    List<Menu> findByMenuCodeIn(List<String> menuCodes);
}
