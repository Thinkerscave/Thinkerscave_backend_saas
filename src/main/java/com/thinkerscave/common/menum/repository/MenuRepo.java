package com.thinkerscave.common.menum.repository;


import com.thinkerscave.common.menum.domain.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuRepo extends JpaRepository<Menu, Long> {
    List<Menu> findByIsActiveTrue(); // only active students

}
