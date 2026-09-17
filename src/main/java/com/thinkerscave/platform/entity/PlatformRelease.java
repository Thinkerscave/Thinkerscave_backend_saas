package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.ReleaseStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_releases")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PlatformRelease extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "release_id", nullable = false, unique = true, length = 80)
    private String releaseId;

    @Column(name = "release_version", nullable = false, length = 50)
    private String releaseVersion;

    @Column(name = "application_version", nullable = false, length = 50)
    private String applicationVersion;

    @Column(name = "target_database_version", nullable = false, length = 50)
    private String targetDatabaseVersion;

    @Column(name = "target_catalog_version", nullable = false, length = 50)
    private String targetCatalogVersion;

    @Column(name = "release_notes", length = 4000)
    private String releaseNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReleaseStatus status = ReleaseStatus.DRAFT;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
