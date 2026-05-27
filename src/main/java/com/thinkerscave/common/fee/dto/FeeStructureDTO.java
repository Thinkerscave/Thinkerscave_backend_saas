package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.enums.GenericStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructureDTO {

    private Long id;

    @NotBlank @Size(max = 128)
    private String name;

    @NotNull
    private Long academicYearId;

    @NotNull
    private Long classId;

    private Long sectionId;

    private Long feePolicyId;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private GenericStatus status;

    @Size(max = 500)
    private String notes;

    @Valid
    private List<FeeStructureItemDTO> items;
}
