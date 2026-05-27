package com.thinkerscave.common.exam.dto;

import com.thinkerscave.common.enums.GenericStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScaleDTO {
    private Long id;

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 500)
    private String description;

    private GenericStatus status;

    @Valid
    @Builder.Default
    private List<GradeBoundaryDTO> boundaries = new ArrayList<>();
}
