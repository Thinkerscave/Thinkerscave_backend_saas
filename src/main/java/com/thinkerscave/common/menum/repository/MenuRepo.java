package com.thinkerscave.common.menum.repository;


import com.thinkerscave.common.menum.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Menu entity.
 * Provides access methods for active menus and lookup by menu code.
 *
 * @author Sandeep
 */
@Repository
public interface MenuRepo extends JpaRepository<Menu, Long> {

    /** Returns all menus where isActive is true. */
    List<Menu> findByIsActiveTrue();

    /** Finds a menu by its unique menu code. */
    Optional<Menu> findByMenuCode(String menuCode);
}