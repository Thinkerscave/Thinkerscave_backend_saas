package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.enums.GenericStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeGroupDTO {

    private Long id;

    @NotBlank @Size(max = 64)
    private String code;

    @NotBlank @Size(max = 128)
    private String name;

    @Size(max = 500)
    private String description;

    private GenericStatus status;
}
