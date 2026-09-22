package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.enums.ReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface PlatformReleaseRepository extends JpaRepository<PlatformRelease, Long> {
    Optional<PlatformRelease> findTopByOrderByCreatedOnDesc();

    Optional<PlatformRelease> findTopByStatusOrderByReleasedAtDesc(ReleaseStatus status);

    @Transactional
    @Modifying
    @Query("""
            UPDATE PlatformRelease r
            SET r.status = com.thinkerscave.platform.enums.ReleaseStatus.READY,
                r.version = r.version + 1
            WHERE r.id = :id
              AND r.status IN (
                  com.thinkerscave.platform.enums.ReleaseStatus.DRAFT,
                  com.thinkerscave.platform.enums.ReleaseStatus.FAILED)
            """)
    int claimExecution(@Param("id") Long id);
}
