package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParentDTO {

    private Long parentId;

    private String parentCode;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String occupation;
}