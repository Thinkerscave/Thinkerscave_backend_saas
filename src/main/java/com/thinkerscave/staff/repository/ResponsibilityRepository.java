package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.Responsibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponsibilityRepository extends JpaRepository<Responsibility, Long> {

    boolean existsByResponsibilityCode(String responsibilityCode);

    Optional<Responsibility> findByResponsibilityCode(String responsibilityCode);

    List<Responsibility> findByActiveTrueOrderByDisplayOrderAscResponsibilityNameAsc();
}
