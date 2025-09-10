package com.thinkerscave.common.menum.repository;

import com.thinkerscave.common.menum.domain.Menu;
import com.thinkerscave.common.menum.domain.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Submenu entity. Provides methods for fetching submenus based
 * on activity, code, and order.
 *
 * @author Sandeep
 */
@Repository
public interface SubMenuRepository extends JpaRepository<SubMenu, Long> {

	List<SubMenu> findByIsActiveTrue();

	/** ✅ Finds a submenu by its unique code. */
	Optional<SubMenu> findBySubMenuCode(String subMenuCode);

	/** ✅ Returns all submenus sorted by subMenuOrder ascending. */
	List<SubMenu> findAllByOrderBySubMenuOrderAsc();

	/** ✅ Returns active submenus sorted by subMenuOrder ascending. */
	List<SubMenu> findByIsActiveTrueOrderBySubMenuOrderAsc();

	/** ✅ Retrieves the maximum subMenuOrder from existing submenus. */
	@Query("SELECT COALESCE(MAX(s.subMenuOrder), 0) FROM SubMenu s")
	Integer findMaxSequence();

	List<SubMenu> findByMenu_MenuIdOrderBySubMenuOrderAsc(Long menuId);

	List<SubMenu> findByMenuAndIsActiveTrueOrderBySubMenuOrderAsc(Menu menu);

}