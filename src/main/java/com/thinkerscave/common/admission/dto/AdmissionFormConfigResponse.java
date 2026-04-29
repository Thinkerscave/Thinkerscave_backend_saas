package com.thinkerscave.common.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdmissionFormConfigResponse {
    private String title;
    private String description;
    private String guidelines;
    private List<AdmissionFormFieldResponse> fields;
}
