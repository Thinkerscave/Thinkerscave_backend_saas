package com.thinkerscave.common.admission.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "admission_form_field")
public class AdmissionFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AdmissionFormTemplate template;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Column(name = "field_type", nullable = false)
    private String fieldType; // TEXT, DROPDOWN, DATE, NUMBER, etc.

    @Column(name = "options", columnDefinition = "TEXT")
    private String options; // For dropdowns, stored as JSON or comma separated

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "validation_pattern")
    private String validationPattern;

    @Column(name = "field_order")
    private Integer fieldOrder;

    @Column(name = "step_index")
    private Integer stepIndex; // Which step in the stepper it belongs to

    @Column(name = "section_title")
    private String sectionTitle;
}
