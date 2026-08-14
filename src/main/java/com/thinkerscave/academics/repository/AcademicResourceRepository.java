package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.AcademicResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicResourceRepository extends JpaRepository<AcademicResource, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndAcademicResourceIdNot(String code, Long id);

    Optional<AcademicResource> findByCodeIgnoreCase(String code);

    List<AcademicResource> findByActiveTrueOrderByNameAsc();
}
