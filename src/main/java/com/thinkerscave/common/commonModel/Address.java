package com.thinkerscave.common.commonModel;

import jakarta.persistence.Column;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "address" ,schema = "public")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String country;
    private String state;
    private String city;
    private String zipCode;

    @Column(columnDefinition = "TEXT")
    private String addressLine;

    // Getters and Setters
}

