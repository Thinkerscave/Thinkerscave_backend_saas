package com.thinkerscave.common.fee.dto;

import com.thinkerscave.common.enums.GenericStatus;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeHeadDTO {

    private Long id;

    @NotBlank @Size(max = 64)
    private String code;

    @NotBlank @Size(max = 128)
    private String name;

    @Size(max = 500)
    private String description;

    private boolean refundable;

    private boolean taxable;

    @Size(max = 64)
    private String glCode;

    private Integer displayOrder;

    private GenericStatus status;
}
