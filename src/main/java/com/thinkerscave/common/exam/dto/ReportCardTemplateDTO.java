package com.thinkerscave.common.exam.dto;

import com.thinkerscave.common.enums.GenericStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCardTemplateDTO {
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 500)
    private String description;

    private String headerText;
    private String footerText;
    private boolean showAttendance;
    private boolean showRank;
    private boolean showRemarks;
    private boolean showCoCurricular;
    private String layoutDefinition;
    private GenericStatus status;
}
