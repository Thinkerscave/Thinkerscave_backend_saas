package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TimetableVersionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimetableVersionEventRepository extends JpaRepository<TimetableVersionEvent, Long> {

    List<TimetableVersionEvent> findByTimetableVersion_TimetableVersionIdOrderByCreatedOnAsc(
            Long versionId);

    @Modifying
    @Query("DELETE FROM TimetableVersionEvent e WHERE e.timetableVersion.timetableVersionId IN :versionIds")
    int deleteByVersionIds(@Param("versionIds") Collection<Long> versionIds);
}
