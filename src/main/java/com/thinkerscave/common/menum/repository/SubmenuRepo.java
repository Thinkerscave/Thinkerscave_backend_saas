package com.thinkerscave.common.menum.repository;

import com.thinkerscave.common.menum.domain.Submenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Submenu entity.
 * Provides methods for fetching submenus based on activity, code, and order.
 *
 * @author Sandeep
 */
@Repository
public interface SubmenuRepo extends JpaRepository<Submenu, Long> {

    /** Returns all active submenus. */
    List<Submenu> findByIsActiveTrue();

    /** Finds a submenu by its code. */
    Optional<Submenu> findBySubmenuCode(String submenuCode);

    /** Returns all submenus sorted by sequence in ascending order. */
    List<Submenu> findAllByOrderBySequenceAsc();

    /** Returns active submenus sorted by sequence in ascending order. */
    List<Submenu> findByIsActiveTrueOrderBySequenceAsc();

    /** Retrieves the maximum sequence number from existing submenus. */
    @Query("SELECT MAX(s.sequence) FROM Submenu s")
    Integer findMaxSequence();
}