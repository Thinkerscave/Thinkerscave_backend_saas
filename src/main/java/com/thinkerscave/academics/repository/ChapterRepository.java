package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByUnit_UnitIdAndActiveOrderByChapterNumberAsc(Long unitId, Boolean active);
}
