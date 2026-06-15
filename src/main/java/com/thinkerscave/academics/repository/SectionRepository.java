package com.thinkerscave.academics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.academics.entity.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

}
