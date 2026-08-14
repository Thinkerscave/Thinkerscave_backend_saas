package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableConflict;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableConflictRepository extends JpaRepository<TimetableConflict, Long> {

    List<TimetableConflict> findByTimetableVersion_TimetableVersionId(Long versionId);

    List<TimetableConflict> findByTimetableVersion_TimetableVersionIdAndStatus(
            Long versionId, TimetableConflictStatus status);

    List<TimetableConflict> findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
            Long versionId, TimetableConflictStatus status);
}
