package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Report-card template — defines header, layout, included sections, footer,
 * signatures. Body is stored as a serialized template (e.g. JSON / HTML
 * fragment) in {@code layout_definition}.
 */
@Entity
@Table(name = "report_card_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_template_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardTemplate extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "header_text", length = 500)
    private String headerText;

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "show_attendance", nullable = false)
    private boolean showAttendance;

    @Column(name = "show_rank", nullable = false)
    private boolean showRank;

    @Column(name = "show_remarks", nullable = false)
    private boolean showRemarks;

    @Column(name = "show_co_curricular", nullable = false)
    private boolean showCoCurricular;

    @Lob
    @Column(name = "layout_definition")
    private String layoutDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
