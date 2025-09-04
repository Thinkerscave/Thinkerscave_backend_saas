package com.thinkerscave.common.commonModel;

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
    @Column(name = "zip_code")
    private String zipCode;

    @Column(name="address_line" ,columnDefinition = "TEXT")
    private String addressLine;


}

