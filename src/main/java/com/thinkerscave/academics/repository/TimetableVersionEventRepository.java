package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableVersionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableVersionEventRepository extends JpaRepository<TimetableVersionEvent, Long> {

    List<TimetableVersionEvent> findByTimetableVersion_TimetableVersionIdOrderByCreatedOnAsc(
            Long versionId);
}
