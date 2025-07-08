package com.thinkerscave.common.menum.repository;

import com.thinkerscave.common.menum.domain.Submenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmenuRepository extends JpaRepository<Submenu, Long> {
    List<Submenu> findByIsActiveTrue();
}
