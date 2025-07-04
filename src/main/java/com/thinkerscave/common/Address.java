package com.thinkerscave.common;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class Address {

    private String country;
    private String state;
    private String city;
    private String zipCode;

    @Column(columnDefinition = "TEXT")
    private String addressLine;
}
