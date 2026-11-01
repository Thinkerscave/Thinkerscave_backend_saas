package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableGenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableGenerationRunRepository extends JpaRepository<TimetableGenerationRun, Long> {

    List<TimetableGenerationRun> findByTimetableVersion_TimetableVersionIdOrderByGenerationRunIdDesc(
            Long versionId);

    Optional<TimetableGenerationRun> findTopByTimetableVersion_TimetableVersionIdOrderByGenerationRunIdDesc(
            Long versionId);

    @Modifying
    @Query("DELETE FROM TimetableGenerationRun r WHERE r.timetableVersion.timetableVersionId IN :versionIds")
    int deleteByVersionIds(@Param("versionIds") Collection<Long> versionIds);
}
