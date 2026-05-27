package com.thinkerscave.common.exam.dto;

import com.thinkerscave.common.exam.domain.ExamStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDTO {
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    @NotNull
    private Long examTypeId;

    @NotNull
    private Long academicYearId;

    @NotNull
    private Long classId;

    private Long sectionId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private Long gradingScaleId;
    private Long reportCardTemplateId;
    private ExamStatus status;
    private String instructions;

    @Valid
    @Builder.Default
    private List<ExamSubjectDTO> subjects = new ArrayList<>();

    @Valid
    @Builder.Default
    private List<ExamScheduleDTO> schedules = new ArrayList<>();
}
