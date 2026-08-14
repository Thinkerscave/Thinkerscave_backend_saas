package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findByTimetableVersion_TimetableVersionId(Long versionId);

    List<TimetableEntry> findByTimetableVersion_TimetableVersionIdAndSection_SectionId(
            Long versionId, Long sectionId);

    List<TimetableEntry> findByTimetableVersion_TimetableVersionIdAndDayOfWeek(
            Long versionId, DayOfWeek dayOfWeek);
}
