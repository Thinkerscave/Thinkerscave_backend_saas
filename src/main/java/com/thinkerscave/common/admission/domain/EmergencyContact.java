package com.thinkerscave.common.admission.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable // Mark this class as embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Column(name = "emergency_contact_name", nullable = false)
    private String name;

    @Column(name = "emergency_contact_number", nullable = false)
    private String number;
}