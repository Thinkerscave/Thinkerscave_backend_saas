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
public class AdmissionFormFieldResponse {
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private List<String> options;
    private Boolean isRequired;
    private String validationPattern;
    private Integer fieldOrder;
    private Integer stepIndex;
    private String sectionTitle;
}
